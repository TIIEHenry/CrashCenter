package nota.android.crash.xp.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashEventTest {

    @Test
    fun `round-trip serialization preserves all fields`() {
        val original = CrashEvent(
            id = "evt-123",
            timestampMs = 1700000000000L,
            packageName = "com.example.app",
            appLabel = "Example App",
            processName = "com.example.app:main",
            exceptionClass = "java.lang.NullPointerException",
            message = "Something went wrong",
            stackTrace = "at com.example.Foo.bar(Foo.java:42)",
            source = "xposed",
            backendWritten = listOf("local", "remote"),
        )

        val jsonLine = original.toJsonLine()
        val restored = CrashEvent.fromJson(jsonLine)

        assertNotNull(restored)
        assertEquals(original.id, restored!!.id)
        assertEquals(original.timestampMs, restored.timestampMs)
        assertEquals(original.packageName, restored.packageName)
        assertEquals(original.appLabel, restored.appLabel)
        assertEquals(original.processName, restored.processName)
        assertEquals(original.exceptionClass, restored.exceptionClass)
        assertEquals(original.message, restored.message)
        assertEquals(original.stackTrace, restored.stackTrace)
        assertEquals(original.source, restored.source)
        assertEquals(original.backendWritten, restored.backendWritten)
    }

    @Test
    fun `round-trip with null optional fields`() {
        val original = CrashEvent(
            id = "evt-456",
            timestampMs = 0L,
            packageName = "",
            appLabel = null,
            processName = null,
            exceptionClass = "Unknown",
            message = null,
            stackTrace = "",
            source = null,
        )

        val jsonLine = original.toJsonLine()
        val restored = CrashEvent.fromJson(jsonLine)

        assertNotNull(restored)
        assertEquals("evt-456", restored!!.id)
        assertEquals(0L, restored.timestampMs)
        assertEquals("", restored.packageName)
        assertNull(restored.appLabel)
        assertNull(restored.processName)
        assertEquals("Unknown", restored.exceptionClass)
        assertNull(restored.message)
        assertEquals("", restored.stackTrace)
        assertNull(restored.source)
        assertTrue(restored.backendWritten.isEmpty())
    }

    @Test
    fun `special characters in stack trace are preserved`() {
        val stackTrace = """at com.example.Foo.bar(Foo.java:42)
	at com.example.Baz.qux(Baz.java:99)
	Caused by: java.lang.IllegalStateException: "quoted" and \ backslash
	at com.example.Inner.method(Inner.java:1)"""

        val original = CrashEvent(
            id = "evt-special",
            timestampMs = 1L,
            packageName = "com.test",
            appLabel = null,
            processName = null,
            exceptionClass = "java.lang.RuntimeException",
            message = "Message with \"quotes\" and \t tabs",
            stackTrace = stackTrace,
            source = null,
        )

        val jsonLine = original.toJsonLine()
        val restored = CrashEvent.fromJson(jsonLine)

        assertNotNull(restored)
        assertEquals(stackTrace, restored!!.stackTrace)
        assertEquals("Message with \"quotes\" and \t tabs", restored.message)
    }

    @Test
    fun `fromJson returns null for empty id`() {
        val json = """{"id":"","timestampMs":0,"packageName":"","exceptionClass":"","stackTrace":""}"""
        assertNull(CrashEvent.fromJson(json))
    }

    @Test
    fun `fromJson returns null for missing id`() {
        val json = """{"timestampMs":0,"packageName":"","exceptionClass":"","stackTrace":""}"""
        assertNull(CrashEvent.fromJson(json))
    }

    @Test
    fun `fromJson returns null for invalid JSON`() {
        assertNull(CrashEvent.fromJson("not json"))
        assertNull(CrashEvent.fromJson(""))
        assertNull(CrashEvent.fromJson("{"))
    }

    @Test
    fun `fromJson handles missing optional fields`() {
        val json = """{"id":"evt-789","timestampMs":100,"packageName":"pkg","exceptionClass":"Ex","stackTrace":"trace"}"""
        val event = CrashEvent.fromJson(json)

        assertNotNull(event)
        assertEquals("evt-789", event!!.id)
        assertEquals(100L, event.timestampMs)
        assertEquals("pkg", event.packageName)
        assertEquals("Ex", event.exceptionClass)
        assertEquals("trace", event.stackTrace)
        assertNull(event.appLabel)
        assertNull(event.processName)
        assertNull(event.message)
        assertNull(event.source)
        assertTrue(event.backendWritten.isEmpty())
    }

    @Test
    fun `fromJson parses backendWritten array`() {
        val json = """{"id":"evt","timestampMs":0,"packageName":"","exceptionClass":"","stackTrace":"","backendWritten":["a","b","c"]}"""
        val event = CrashEvent.fromJson(json)

        assertNotNull(event)
        assertEquals(listOf("a", "b", "c"), event!!.backendWritten)
    }

    @Test
    fun `fromJson ignores empty strings in backendWritten`() {
        val json = """{"id":"evt","timestampMs":0,"packageName":"","exceptionClass":"","stackTrace":"","backendWritten":["a","","c"]}"""
        val event = CrashEvent.fromJson(json)

        assertNotNull(event)
        assertEquals(listOf("a", "c"), event!!.backendWritten)
    }

    @Test
    fun `fromJson handles empty backendWritten array`() {
        val json = """{"id":"evt","timestampMs":0,"packageName":"","exceptionClass":"","stackTrace":"","backendWritten":[]}"""
        val event = CrashEvent.fromJson(json)

        assertNotNull(event)
        assertTrue(event!!.backendWritten.isEmpty())
    }

    @Test
    fun `fromJson handles missing backendWritten`() {
        val json = """{"id":"evt","timestampMs":0,"packageName":"","exceptionClass":"","stackTrace":""}"""
        val event = CrashEvent.fromJson(json)

        assertNotNull(event)
        assertTrue(event!!.backendWritten.isEmpty())
    }

    @Test
    fun `shortExceptionClass returns class name without package`() {
        val event = CrashEvent(
            id = "1",
            timestampMs = 0L,
            packageName = "",
            appLabel = null,
            processName = null,
            exceptionClass = "java.lang.NullPointerException",
            message = null,
            stackTrace = "",
            source = null,
        )
        assertEquals("NullPointerException", event.shortExceptionClass)
    }

    @Test
    fun `shortExceptionClass returns full string when no dot`() {
        val event = CrashEvent(
            id = "1",
            timestampMs = 0L,
            packageName = "",
            appLabel = null,
            processName = null,
            exceptionClass = "CustomException",
            message = null,
            stackTrace = "",
            source = null,
        )
        assertEquals("CustomException", event.shortExceptionClass)
    }

    @Test
    fun `withBackendWritten returns new instance with updated backends`() {
        val original = CrashEvent(
            id = "1",
            timestampMs = 0L,
            packageName = "",
            appLabel = null,
            processName = null,
            exceptionClass = "",
            message = null,
            stackTrace = "",
            source = null,
        )
        val updated = original.withBackendWritten(listOf("a", "b", "a"))

        assertEquals(listOf("a", "b"), updated.backendWritten)
        // Original should be unchanged
        assertTrue(original.backendWritten.isEmpty())
    }

    @Test
    fun `toJsonLine omits null fields`() {
        val event = CrashEvent(
            id = "evt",
            timestampMs = 0L,
            packageName = "pkg",
            appLabel = null,
            processName = null,
            exceptionClass = "Ex",
            message = null,
            stackTrace = "trace",
            source = null,
        )
        val json = event.toJsonLine()
        assertTrue(!json.contains("appLabel"))
        assertTrue(!json.contains("processName"))
        assertTrue(!json.contains("message"))
        assertTrue(!json.contains("source"))
        assertTrue(!json.contains("backendWritten"))
    }

    @Test
    fun `toJsonLine includes backendWritten when non-empty`() {
        val event = CrashEvent(
            id = "evt",
            timestampMs = 0L,
            packageName = "pkg",
            appLabel = null,
            processName = null,
            exceptionClass = "Ex",
            message = null,
            stackTrace = "trace",
            source = null,
            backendWritten = listOf("local"),
        )
        val json = event.toJsonLine()
        assertTrue(json.contains("backendWritten"))
    }

    @Test
    fun `fromJson handles unicode in message and stack trace`() {
        val original = CrashEvent(
            id = "evt-unicode",
            timestampMs = 0L,
            packageName = "pkg",
            appLabel = null,
            processName = null,
            exceptionClass = "Ex",
            message = "Error: 中文测试",
            stackTrace = "at 中文方法(Foo.java:1)",
            source = null,
        )
        val jsonLine = original.toJsonLine()
        val restored = CrashEvent.fromJson(jsonLine)

        assertNotNull(restored)
        assertEquals("Error: 中文测试", restored!!.message)
        assertEquals("at 中文方法(Foo.java:1)", restored.stackTrace)
    }

    @Test
    fun `fromJson uses defaults for missing required fields`() {
        val json = """{"id":"evt"}"""
        val event = CrashEvent.fromJson(json)

        assertNotNull(event)
        assertEquals("evt", event!!.id)
        assertEquals(0L, event.timestampMs)
        assertEquals("", event.packageName)
        assertEquals("Unknown", event.exceptionClass)
        assertEquals("", event.stackTrace)
    }
}
