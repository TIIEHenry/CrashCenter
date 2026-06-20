package nota.android.crash.xp.app.data

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class FileCrashLogRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var repo: FileCrashLogRepository
    private lateinit var eventsFile: File

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        // Create the repo normally (it will use app's filesDir)
        repo = FileCrashLogRepository(context)
        // Get the file path the repo uses
        eventsFile = FileCrashLogRepository.eventsFile(context)
        // Ensure parent dir exists
        eventsFile.parentFile?.mkdirs()
        // Clear any existing file
        eventsFile.delete()
    }

    private fun writeEvent(event: CrashEvent) {
        eventsFile.parentFile?.mkdirs()
        eventsFile.appendText(event.toJsonLine() + "\n")
    }

    private fun event(
        id: String,
        timestampMs: Long = System.currentTimeMillis(),
        packageName: String = "com.example",
        exceptionClass: String = "RuntimeException",
        message: String? = "test",
        source: String? = "xposed",
    ) = CrashEvent(
        id = id,
        timestampMs = timestampMs,
        packageName = packageName,
        exceptionClass = exceptionClass,
        message = message,
        source = source,
    )

    @Test
    fun writeAndReadBack() {
        val e1 = event(id = "1", timestampMs = 1000)
        val e2 = event(id = "2", timestampMs = 2000)
        writeEvent(e1)
        writeEvent(e2)

        val all = repo.getAll(CrashFilter(), limit = 100, offset = 0)
        assertEquals(2, all.size)
        assertEquals("1", all[0].id)
        assertEquals("2", all[1].id)
    }

    @Test
    fun getByIdReturnsCorrectEvent() {
        val e1 = event(id = "a", message = "first")
        val e2 = event(id = "b", message = "second")
        writeEvent(e1)
        writeEvent(e2)

        assertEquals("first", repo.getById("a")?.message)
        assertEquals("second", repo.getById("b")?.message)
        assertNull(repo.getById("c"))
    }

    @Test
    fun getCountReturnsCorrectCount() {
        writeEvent(event(id = "1", packageName = "pkg.a"))
        writeEvent(event(id = "2", packageName = "pkg.b"))
        writeEvent(event(id = "3", packageName = "pkg.a"))

        assertEquals(3, repo.getCount(CrashFilter()))
        assertEquals(2, repo.getCount(CrashFilter(packageName = "pkg.a")))
        assertEquals(1, repo.getCount(CrashFilter(packageName = "pkg.b")))
        assertEquals(0, repo.getCount(CrashFilter(packageName = "pkg.c")))
    }

    @Test
    fun streamingStopsEarlyWhenLimitReached() {
        repeat(10) { i ->
            writeEvent(event(id = "$i", timestampMs = (i * 1000).toLong()))
        }

        val result = repo.getAll(CrashFilter(), limit = 3, offset = 0)
        assertEquals(3, result.size)
        assertEquals("0", result[0].id)
        assertEquals("1", result[1].id)
        assertEquals("2", result[2].id)
    }

    @Test
    fun cacheReturnsEventsWithoutReReadingFile() {
        val e1 = event(id = "cache1", message = "cached")
        writeEvent(e1)

        // First call populates cache
        val first = repo.getById("cache1")
        assertNotNull(first)
        assertEquals("cached", first?.message)

        // Delete the file - if cache works, getById still returns the event
        eventsFile.delete()
        val cached = repo.getById("cache1")
        assertNotNull("Cache should return event without reading file", cached)
        assertEquals("cached", cached?.message)
    }

    @Test
    fun threadSafetyUnderConcurrentReads() {
        repeat(50) { i ->
            writeEvent(event(id = "concurrent-$i", timestampMs = i.toLong()))
        }

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(100)
        val errors = AtomicInteger(0)

        repeat(100) {
            executor.submit {
                try {
                    val all = repo.getAll(CrashFilter(), limit = 50, offset = 0)
                    assertEquals(50, all.size)
                    val count = repo.getCount(CrashFilter())
                    assertEquals(50, count)
                    val byId = repo.getById("concurrent-25")
                    assertNotNull(byId)
                } catch (e: Throwable) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("Timed out waiting for threads", latch.await(10, TimeUnit.SECONDS))
        assertEquals("Concurrent read errors occurred", 0, errors.get())
        executor.shutdown()
    }

    @Test
    fun emptyFileReturnsEmptyList() {
        val all = repo.getAll(CrashFilter(), limit = 100, offset = 0)
        assertTrue(all.isEmpty())
        assertEquals(0, repo.getCount(CrashFilter()))
        assertNull(repo.getById("anything"))
    }

    @Test
    fun offsetSkipsEvents() {
        repeat(5) { i ->
            writeEvent(event(id = "$i", timestampMs = (i * 1000).toLong()))
        }

        val result = repo.getAll(CrashFilter(), limit = 10, offset = 2)
        assertEquals(3, result.size)
        assertEquals("2", result[0].id)
        assertEquals("3", result[1].id)
        assertEquals("4", result[2].id)
    }

    @Test
    fun filterByPackageName() {
        writeEvent(event(id = "1", packageName = "com.foo"))
        writeEvent(event(id = "2", packageName = "com.bar"))
        writeEvent(event(id = "3", packageName = "com.foo"))

        val result = repo.getAll(CrashFilter(packageName = "com.foo"), limit = 100, offset = 0)
        assertEquals(2, result.size)
        assertTrue(result.all { it.packageName == "com.foo" })
    }

    @Test
    fun filterBySource() {
        writeEvent(event(id = "1", source = "xposed"))
        writeEvent(event(id = "2", source = "native"))
        writeEvent(event(id = "3", source = "xposed"))

        val result = repo.getAll(CrashFilter(source = "xposed"), limit = 100, offset = 0)
        assertEquals(2, result.size)
        assertTrue(result.all { it.source == "xposed" })
    }

    @Test
    fun filterByTimeRange() {
        writeEvent(event(id = "1", timestampMs = 1000))
        writeEvent(event(id = "2", timestampMs = 2000))
        writeEvent(event(id = "3", timestampMs = 3000))

        val result = repo.getAll(CrashFilter(sinceMs = 1500, untilMs = 2500), limit = 100, offset = 0)
        assertEquals(1, result.size)
        assertEquals("2", result[0].id)
    }

    @Test
    fun filterByQuery() {
        writeEvent(event(id = "1", packageName = "com.foo", exceptionClass = "NullPointerException"))
        writeEvent(event(id = "2", packageName = "com.bar", exceptionClass = "IllegalStateException"))
        writeEvent(event(id = "3", packageName = "com.foo", exceptionClass = "IllegalArgumentException"))

        val result = repo.getAll(CrashFilter(query = "com.foo"), limit = 100, offset = 0)
        assertEquals(2, result.size)
        assertTrue(result.all { it.packageName == "com.foo" })

        val result2 = repo.getAll(CrashFilter(query = "Illegal"), limit = 100, offset = 0)
        assertEquals(2, result2.size)
    }

    @Test
    fun deleteByIdRemovesEvent() {
        writeEvent(event(id = "1"))
        writeEvent(event(id = "2"))
        writeEvent(event(id = "3"))

        assertTrue(repo.deleteById("2"))
        assertEquals(2, repo.getCount(CrashFilter()))
        assertNull(repo.getById("2"))
        assertNotNull(repo.getById("1"))
        assertNotNull(repo.getById("3"))
    }

    @Test
    fun deleteByIdReturnsFalseWhenNotFound() {
        writeEvent(event(id = "1"))
        assertFalse(repo.deleteById("99"))
        assertEquals(1, repo.getCount(CrashFilter()))
    }

    @Test
    fun deleteByIdDeletesFileWhenLastEventRemoved() {
        writeEvent(event(id = "1"))
        assertTrue(repo.deleteById("1"))
        assertFalse(eventsFile.exists())
        assertEquals(0, repo.getCount(CrashFilter()))
    }

    @Test
    fun deleteByIdInvalidatesCache() {
        val e1 = event(id = "cache-del", message = "cached")
        writeEvent(e1)
        repo.getById("cache-del") // populate cache
        eventsFile.delete() // file gone, but cache still has it

        // Before deleteById, cache would still serve the event
        repo.deleteById("cache-del")
        assertNull(repo.getById("cache-del"))
    }

    @Test
    fun clearRemovesAllEvents() {
        writeEvent(event(id = "1"))
        writeEvent(event(id = "2"))

        repo.clear()
        assertEquals(0, repo.getCount(CrashFilter()))
        assertNull(repo.getById("1"))
        assertFalse(eventsFile.exists())
    }

    @Test
    fun clearInvalidatesCache() {
        val e1 = event(id = "cache-clr", message = "cached")
        writeEvent(e1)
        repo.getById("cache-clr") // populate cache
        eventsFile.delete() // file gone

        repo.clear()
        assertNull(repo.getById("cache-clr"))
    }

    @Test
    fun threadSafetyUnderConcurrentDeletes() {
        repeat(50) { i ->
            writeEvent(event(id = "del-$i", timestampMs = i.toLong()))
        }

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(50)
        val errors = AtomicInteger(0)

        repeat(50) { i ->
            executor.submit {
                try {
                    repo.deleteById("del-$i")
                } catch (e: Throwable) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("Timed out waiting for threads", latch.await(10, TimeUnit.SECONDS))
        assertEquals("Concurrent delete errors occurred", 0, errors.get())
        assertEquals(0, repo.getCount(CrashFilter()))
        executor.shutdown()
    }

    @Test
    fun invalidJsonLinesAreSkipped() {
        eventsFile.parentFile?.mkdirs()
        eventsFile.writeText("{\"id\":\"1\",\"timestampMs\":100,\"packageName\":\"a\",\"exceptionClass\":\"E\"}\n")
        eventsFile.appendText("this is not json\n")
        eventsFile.appendText("{\"id\":\"2\",\"timestampMs\":200,\"packageName\":\"b\",\"exceptionClass\":\"F\"}\n")

        val all = repo.getAll(CrashFilter(), limit = 100, offset = 0)
        assertEquals(2, all.size)
        assertEquals("1", all[0].id)
        assertEquals("2", all[1].id)
    }
}
