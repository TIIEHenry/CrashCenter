package nota.android.crash.xp.app.observe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatParserTest {

    @Test
    fun `parse empty text returns empty list`() {
        assertEquals(emptyList<LogcatEntry>(), LogcatParser.parse(""))
        assertEquals(emptyList<LogcatEntry>(), LogcatParser.parse(null))
    }

    @Test
    fun `parse single line extracts all fields`() {
        val line = "06-21 14:30:22.123  1234  5678 E AndroidRuntime: FATAL EXCEPTION: main"
        val entries = LogcatParser.parse(line)

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("06-21 14:30:22.123", entry.timestamp)
        assertEquals("1234", entry.pid)
        assertEquals("5678", entry.tid)
        assertEquals(LogcatLevel.ERROR, entry.level)
        assertEquals("AndroidRuntime", entry.tag)
        assertEquals("FATAL EXCEPTION: main", entry.message)
    }

    @Test
    fun `parse multiple lines produces multiple entries`() {
        val text = """
            06-21 14:30:22.123  1234  5678 E AndroidRuntime: FATAL EXCEPTION: main
            06-21 14:30:22.124  1234  5678 E AndroidRuntime: java.lang.NullPointerException
            06-21 14:30:22.200  2000  2001 I MainActivity: App started
        """.trimIndent()

        val entries = LogcatParser.parse(text)
        assertEquals(3, entries.size)
        assertEquals("AndroidRuntime", entries[0].tag)
        assertEquals("AndroidRuntime", entries[1].tag)
        assertEquals("MainActivity", entries[2].tag)
        assertEquals(LogcatLevel.INFO, entries[2].level)
    }

    @Test
    fun `continuation lines are appended to previous entry`() {
        val text = """
            06-21 14:30:22.123  1234  5678 E AndroidRuntime: FATAL EXCEPTION: main
                at com.example.MyClass.method(MyClass.java:42)
                at com.example.MyClass.run(MyClass.java:10)
        """.trimIndent()

        val entries = LogcatParser.parse(text)
        assertEquals(1, entries.size)
        assertTrue(entries[0].message.contains("at com.example.MyClass.method"))
        assertTrue(entries[0].message.contains("at com.example.MyClass.run"))
    }

    @Test
    fun `parse all log levels`() {
        val text = """
            06-21 00:00:00.000  100  100 V TagV: verbose
            06-21 00:00:00.100  100  100 D TagD: debug
            06-21 00:00:00.200  100  100 I TagI: info
            06-21 00:00:00.300  100  100 W TagW: warning
            06-21 00:00:00.400  100  100 E TagE: error
            06-21 00:00:00.500  100  100 F TagF: fatal
        """.trimIndent()

        val entries = LogcatParser.parse(text)
        assertEquals(6, entries.size)
        assertEquals(LogcatLevel.VERBOSE, entries[0].level)
        assertEquals(LogcatLevel.DEBUG, entries[1].level)
        assertEquals(LogcatLevel.INFO, entries[2].level)
        assertEquals(LogcatLevel.WARNING, entries[3].level)
        assertEquals(LogcatLevel.ERROR, entries[4].level)
        assertEquals(LogcatLevel.FATAL, entries[5].level)
    }

    @Test
    fun `filterCrashRelated finds FATAL entries`() {
        val entries = listOf(
            entry(LogcatLevel.ERROR, "AndroidRuntime", "FATAL EXCEPTION: main"),
            entry(LogcatLevel.INFO, "MyApp", "Started"),
        )

        val crash = LogcatParser.filterCrashRelated(entries)
        assertEquals(1, crash.size)
        assertEquals("AndroidRuntime", crash[0].tag)
    }

    @Test
    fun `filterCrashRelated finds XposedBridge entries`() {
        val entries = listOf(
            entry(LogcatLevel.ERROR, "XposedBridge", "java.lang.RuntimeException"),
            entry(LogcatLevel.INFO, "MyApp", "Started"),
        )

        val crash = LogcatParser.filterCrashRelated(entries)
        assertEquals(1, crash.size)
    }

    @Test
    fun `filterCrashRelated finds hook markers`() {
        val entries = listOf(
            entry(LogcatLevel.INFO, "CrashCenter", "catch package: com.example.app"),
            entry(LogcatLevel.INFO, "XposedEntry", "selfCheck:pkg: nota.android.crash.xp.app"),
            entry(LogcatLevel.INFO, "MyApp", "normal log"),
        )

        val crash = LogcatParser.filterCrashRelated(entries)
        assertEquals(2, crash.size)
    }

    @Test
    fun `filterCrashRelated finds native crash hints`() {
        val entries = listOf(
            entry(LogcatLevel.ERROR, "DEBUG", "Fatal signal 11 (SIGSEGV)"),
            entry(LogcatLevel.DEBUG, "DEBUG", "backtrace: #00 pc 0x00001234"),
        )

        val crash = LogcatParser.filterCrashRelated(entries)
        assertEquals(2, crash.size)
    }

    @Test
    fun `filterCrashRelated finds process death`() {
        val entries = listOf(
            entry(LogcatLevel.INFO, "ActivityManager", "Process com.example.app has died"),
        )

        val crash = LogcatParser.filterCrashRelated(entries)
        assertEquals(1, crash.size)
        assertTrue(!LogcatParser.isAnrHint(crash[0]))
    }

    @Test
    fun `filterCrashRelated finds ANR in message case insensitively`() {
        val entries = listOf(
            entry(LogcatLevel.ERROR, "ActivityManager", "ANR in com.example.app (com.example.app/.MainActivity)"),
            entry(LogcatLevel.ERROR, "ActivityManager", "anr in com.example.other"),
        )

        val crash = LogcatParser.filterCrashRelated(entries)
        assertEquals(2, crash.size)
        assertTrue(crash.all { LogcatParser.isAnrHint(it) })
    }

    @Test
    fun `filterCrashRelated finds am_anr in events buffer text`() {
        val entries = listOf(
            entry(LogcatLevel.INFO, "eventlog", "am_anr: [0,12345,com.example.app,...]"),
        )

        val crash = LogcatParser.filterCrashRelated(entries)
        assertEquals(1, crash.size)
        assertTrue(LogcatParser.isAnrHint(crash[0]))
    }

    @Test
    fun `filterCrashRelated finds ActivityManager ANR annotations`() {
        val entries = listOf(
            entry(LogcatLevel.ERROR, "ActivityManager", "Input dispatching timed out"),
            entry(LogcatLevel.ERROR, "ActivityManager", "App is not responding. Waited 5000ms"),
        )

        val crash = LogcatParser.filterCrashRelated(entries)
        assertEquals(2, crash.size)
        assertTrue(crash.all { LogcatParser.isAnrHint(it) })
    }

    @Test
    fun `isAnrHint false for unrelated ActivityManager lines`() {
        val entry = entry(LogcatLevel.INFO, "ActivityManager", "Start proc com.example.app for activity")
        assertTrue(!LogcatParser.isAnrHint(entry))
        assertTrue(!LogcatParser.isCrashRelated(entry))
    }

    @Test
    fun `displaySummary truncates long messages`() {
        val entry = LogcatEntry(
            timestamp = "06-21 00:00:00.000",
            pid = "1", tid = "1",
            level = LogcatLevel.ERROR,
            tag = "Tag",
            message = "A".repeat(200),
        )
        assertTrue(entry.displaySummary.length <= 120)
        assertTrue(entry.displaySummary.endsWith("..."))
    }

    @Test
    fun `displaySummary shows first line of multiline message`() {
        val entry = LogcatEntry(
            timestamp = "06-21 00:00:00.000",
            pid = "1", tid = "1",
            level = LogcatLevel.ERROR,
            tag = "Tag",
            message = "First line\nSecond line\nThird line",
        )
        assertEquals("First line", entry.displaySummary)
    }

    @Test
    fun `rawLine reconstructs the logcat format`() {
        val entry = LogcatEntry(
            timestamp = "06-21 14:30:22.123",
            pid = "1234", tid = "5678",
            level = LogcatLevel.ERROR,
            tag = "AndroidRuntime",
            message = "FATAL EXCEPTION: main",
        )
        assertEquals("06-21 14:30:22.123 1234 5678 E AndroidRuntime: FATAL EXCEPTION: main", entry.rawLine)
    }

    // ─── Helper ───

    private fun entry(level: LogcatLevel, tag: String, message: String) = LogcatEntry(
        timestamp = "06-21 00:00:00.000",
        pid = "1000", tid = "1000",
        level = level, tag = tag, message = message,
    )
}
