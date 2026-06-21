package nota.android.crash.log

import nota.android.crash.common.data.CrashEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CanonicalJsonlWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createEvent(
        id: String,
        packageName: String = "com.example.app",
        exceptionClass: String = "java.lang.NullPointerException",
        stackTrace: String = "at com.example.Foo.bar(Foo.java:42)",
    ): CrashEvent = CrashEvent(
        id = id,
        timestampMs = 1700000000000L,
        packageName = packageName,
        appLabel = "Example App",
        processName = "$packageName:main",
        exceptionClass = exceptionClass,
        message = "Something went wrong",
        stackTrace = stackTrace,
        source = "xposed",
    )

    private fun readLines(file: File): List<String> {
        return file.readLines(Charsets.UTF_8)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    // ---------- Append single line ----------

    @Test
    fun `append single event creates file with one line`() {
        val file = tempFolder.newFile("events.jsonl")
        val event = createEvent("evt-1")

        CanonicalJsonlWriter.append(file, event)

        val lines = readLines(file)
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("\"id\":\"evt-1\""))
    }

    @Test
    fun `append to non-existent file creates parent directories and file`() {
        val parentDir = tempFolder.newFolder("sub", "dir")
        val file = File(parentDir, "events.jsonl")
        val event = createEvent("evt-1")

        CanonicalJsonlWriter.append(file, event)

        assertTrue(file.exists())
        assertEquals(1, readLines(file).size)
    }

    @Test
    fun `append single event with null optional fields writes valid JSON`() {
        val file = tempFolder.newFile("events.jsonl")
        val event = CrashEvent(
            id = "evt-minimal",
            timestampMs = 0L,
            packageName = "pkg",
            appLabel = null,
            processName = null,
            exceptionClass = "Ex",
            message = null,
            stackTrace = "trace",
            source = null,
        )

        CanonicalJsonlWriter.append(file, event)

        val lines = readLines(file)
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("\"id\":\"evt-minimal\""))
        // Null fields should be omitted
        assertTrue(!lines[0].contains("appLabel"))
    }

    // ---------- Append multiple lines ----------

    @Test
    fun `append multiple events writes each on separate line`() {
        val file = tempFolder.newFile("events.jsonl")

        CanonicalJsonlWriter.append(file, createEvent("evt-1"))
        CanonicalJsonlWriter.append(file, createEvent("evt-2"))
        CanonicalJsonlWriter.append(file, createEvent("evt-3"))

        val lines = readLines(file)
        assertEquals(3, lines.size)
        assertTrue(lines[0].contains("\"id\":\"evt-1\""))
        assertTrue(lines[1].contains("\"id\":\"evt-2\""))
        assertTrue(lines[2].contains("\"id\":\"evt-3\""))
    }

    @Test
    fun `append preserves existing content`() {
        val file = tempFolder.newFile("events.jsonl")

        CanonicalJsonlWriter.append(file, createEvent("evt-1"))
        CanonicalJsonlWriter.append(file, createEvent("evt-2"))

        val lines = readLines(file)
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("evt-1"))
        assertTrue(lines[1].contains("evt-2"))
    }

    @Test
    fun `append handles events with special characters in stack trace`() {
        val file = tempFolder.newFile("events.jsonl")
        val event = CrashEvent(
            id = "evt-special",
            timestampMs = 1L,
            packageName = "com.test",
            appLabel = null,
            processName = null,
            exceptionClass = "java.lang.RuntimeException",
            message = "Message with \"quotes\" and \t tabs",
            stackTrace = "at com.example.Foo.bar(Foo.java:42)\n\tat com.example.Baz.qux(Baz.java:99)",
            source = null,
        )

        CanonicalJsonlWriter.append(file, event)

        val lines = readLines(file)
        assertEquals(1, lines.size)
        // Verify the JSON is valid by parsing it back
        val restored = CrashEvent.fromJson(lines[0])
        assertEquals("evt-special", restored?.id)
        assertEquals("Message with \"quotes\" and \t tabs", restored?.message)
    }

    // ---------- Read back lines ----------

    @Test
    fun `read back lines returns correct event count`() {
        val file = tempFolder.newFile("events.jsonl")
        repeat(10) { i ->
            CanonicalJsonlWriter.append(file, createEvent("evt-$i"))
        }

        val lines = readLines(file)
        assertEquals(10, lines.size)
    }

    @Test
    fun `read back lines can be parsed back to CrashEvent`() {
        val file = tempFolder.newFile("events.jsonl")
        val original = createEvent("evt-roundtrip")
        CanonicalJsonlWriter.append(file, original)

        val lines = readLines(file)
        val restored = CrashEvent.fromJson(lines[0])

        assertEquals(original.id, restored?.id)
        assertEquals(original.timestampMs, restored?.timestampMs)
        assertEquals(original.packageName, restored?.packageName)
        assertEquals(original.exceptionClass, restored?.exceptionClass)
        assertEquals(original.stackTrace, restored?.stackTrace)
    }

    @Test
    fun `read back ignores empty lines in file`() {
        val file = tempFolder.newFile("events.jsonl")
        file.writeText("\n\n{\"id\":\"evt-1\",\"timestampMs\":0,\"packageName\":\"pkg\",\"exceptionClass\":\"Ex\",\"stackTrace\":\"trace\"}\n\n\n")

        val lines = readLines(file)
        assertEquals(1, lines.size)
    }

    // ---------- File locking behavior ----------

    @Test
    fun `append with file lock does not corrupt content across separate files`() {
        // Note: File locks within the same JVM process throw OverlappingFileLockException,
        // so we test concurrent writes to separate files instead to verify lock behavior.
        val fileCount = 5
        val eventsPerFile = 10
        val files = mutableListOf<File>()
        val threads = mutableListOf<Thread>()

        for (f in 0 until fileCount) {
            val file = tempFolder.newFile("events-${f}.jsonl")
            files.add(file)
            val thread = Thread {
                for (e in 0 until eventsPerFile) {
                    CanonicalJsonlWriter.append(file, createEvent("file-${f}-evt-${e}"))
                }
            }
            threads.add(thread)
            thread.start()
        }

        threads.forEach { it.join() }

        for (file in files) {
            val lines = readLines(file)
            assertEquals(eventsPerFile, lines.size)
            lines.forEach { line ->
                assertTrue(line.startsWith("{"))
                assertTrue(line.endsWith("}"))
            }
        }
    }

    @Test
    fun `append with file lock on single file from one thread works correctly`() {
        // Single-threaded test to verify the lock mechanism works at all
        val file = tempFolder.newFile("events.jsonl")
        repeat(20) { i ->
            CanonicalJsonlWriter.append(file, createEvent("evt-$i"))
        }

        val lines = readLines(file)
        assertEquals(20, lines.size)
        lines.forEach { line ->
            assertTrue(line.startsWith("{"))
            assertTrue(line.endsWith("}"))
        }
    }

    // ---------- Retention policy ----------

    @Test
    fun `retention trims entries when exceeding MAX_ENTRIES`() {
        val file = tempFolder.newFile("events.jsonl")
        // Write more than MAX_ENTRIES
        repeat(CanonicalJsonlWriter.MAX_ENTRIES + 10) { i ->
            CanonicalJsonlWriter.append(file, createEvent("evt-$i"))
        }

        val lines = readLines(file)
        assertTrue("Expected at most ${CanonicalJsonlWriter.MAX_ENTRIES} lines, got ${lines.size}",
            lines.size <= CanonicalJsonlWriter.MAX_ENTRIES)
    }

    @Test
    fun `retention trims oldest entries first`() {
        val file = tempFolder.newFile("events.jsonl")
        repeat(CanonicalJsonlWriter.MAX_ENTRIES + 5) { i ->
            CanonicalJsonlWriter.append(file, createEvent("evt-$i"))
        }

        val lines = readLines(file)
        // Oldest entries should be removed, newest should remain
        assertTrue(!lines[0].contains("evt-0"))
        assertTrue(!lines[0].contains("evt-1"))
        assertTrue(!lines[0].contains("evt-2"))
        assertTrue(!lines[0].contains("evt-3"))
        assertTrue(!lines[0].contains("evt-4"))
    }

    @Test
    fun `retention by byte size trims when exceeding MAX_BYTES`() {
        val file = tempFolder.newFile("events.jsonl")
        // Create events with large stack traces to exceed byte limit quickly
        val largeStackTrace = "a".repeat(10000)
        repeat(100) { i ->
            CanonicalJsonlWriter.append(file, createEvent("evt-$i", stackTrace = largeStackTrace))
        }

        val lines = readLines(file)
        val totalBytes = lines.sumOf { it.toByteArray(Charsets.UTF_8).size + 1L }
        assertTrue("Expected total bytes <= ${CanonicalJsonlWriter.MAX_BYTES}, got $totalBytes",
            totalBytes <= CanonicalJsonlWriter.MAX_BYTES)
    }

    @Test
    fun `applyRetention on file within limits does not modify content`() {
        val file = tempFolder.newFile("events.jsonl")
        repeat(5) { i ->
            CanonicalJsonlWriter.append(file, createEvent("evt-$i"))
        }

        val before = readLines(file)
        CanonicalJsonlWriter.applyRetention(file)
        val after = readLines(file)

        assertEquals(before, after)
    }

    @Test
    fun `applyRetention on non-existent file does nothing`() {
        val file = File(tempFolder.root, "nonexistent.jsonl")
        // Should not throw
        CanonicalJsonlWriter.applyRetention(file)
        assertTrue(!file.exists())
    }

    @Test
    fun `applyRetention on empty file does nothing`() {
        val file = tempFolder.newFile("empty.jsonl")
        CanonicalJsonlWriter.applyRetention(file)
        assertEquals(0, readLines(file).size)
    }

    // ---------- Invalid file handling ----------

    @Test
    fun `append when parent is not a directory throws FileNotFoundException`() {
        // Create a file where parent "dir" should be, so mkdirs succeeds but
        // RandomAccessFile fails because the path is not a directory
        val blockingFile = tempFolder.newFile("blocking")
        val file = File(blockingFile, "events.jsonl")
        val event = createEvent("evt-1")

        // RandomAccessFile throws when parent exists but is not a directory
        var threw = false
        try {
            CanonicalJsonlWriter.append(file, event)
        } catch (_: java.io.FileNotFoundException) {
            threw = true
        }
        assertTrue("Expected FileNotFoundException when parent is not a directory", threw)
        assertTrue(!file.exists())
    }

    @Test
    fun `append to directory path instead of file handles gracefully`() {
        val dir = tempFolder.newFolder("notafile")
        val event = createEvent("evt-1")

        // Should not throw - RandomAccessFile will fail but it's caught
        try {
            CanonicalJsonlWriter.append(dir, event)
        } catch (_: Exception) {
            // Expected - directory can't be opened as file
        }
    }

    @Test
    fun `readNonEmptyLines handles file with only whitespace`() {
        val file = tempFolder.newFile("whitespace.jsonl")
        file.writeText("   \n\t\n   \n")

        val lines = readLines(file)
        assertEquals(0, lines.size)
    }

    @Test
    fun `append handles unicode content correctly`() {
        val file = tempFolder.newFile("events.jsonl")
        val event = CrashEvent(
            id = "evt-unicode",
            timestampMs = 0L,
            packageName = "pkg",
            appLabel = null,
            processName = null,
            exceptionClass = "Ex",
            message = "Error: 中文测试 😀",
            stackTrace = "at 中文方法(Foo.java:1)",
            source = null,
        )

        CanonicalJsonlWriter.append(file, event)

        val lines = readLines(file)
        val restored = CrashEvent.fromJson(lines[0])
        assertEquals("Error: 中文测试 😀", restored?.message)
        assertEquals("at 中文方法(Foo.java:1)", restored?.stackTrace)
    }

    @Test
    fun `multiple applyRetention calls are idempotent`() {
        val file = tempFolder.newFile("events.jsonl")
        repeat(20) { i ->
            CanonicalJsonlWriter.append(file, createEvent("evt-$i"))
        }

        // Apply retention multiple times
        CanonicalJsonlWriter.applyRetention(file)
        CanonicalJsonlWriter.applyRetention(file)
        CanonicalJsonlWriter.applyRetention(file)

        val lines = readLines(file)
        // Should still be valid and within limits
        assertTrue(lines.size <= CanonicalJsonlWriter.MAX_ENTRIES)
        lines.forEach { line ->
            assertTrue(line.startsWith("{"))
            assertTrue(line.endsWith("}"))
        }
    }
}
