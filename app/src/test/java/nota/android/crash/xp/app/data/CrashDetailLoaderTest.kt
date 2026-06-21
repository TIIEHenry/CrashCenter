package nota.android.crash.xp.app.data

import nota.android.crash.common.data.CrashEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CrashDetailLoaderTest {

    // -- stackTraceFrom --------------------------------------------------------

    @Test
    fun `stackTraceFrom returns stackTrace directly when non-blank`() {
        val trace = "java.lang.RuntimeException: boom\n\tat com.example.Main.main(Main.java:10)"
        val event = event(stackTrace = trace)

        assertEquals(trace, CrashDetailLoader.stackTraceFrom(event))
    }

    @Test
    fun `stackTraceFrom falls back to formatFallback when stackTrace is blank`() {
        val event = event(
            exceptionClass = "java.lang.NullPointerException",
            message = "obj is null",
            packageName = "com.example.app",
            id = "abc-123",
        )

        val result = CrashDetailLoader.stackTraceFrom(event)

        assertEquals(
            "java.lang.NullPointerException: obj is null\npackage=com.example.app id=abc-123",
            result,
        )
    }

    @Test
    fun `stackTraceFrom falls back when stackTrace is empty`() {
        val event = event(
            stackTrace = "",
            exceptionClass = "Unknown",
            message = null,
            packageName = "pkg",
            id = "1",
        )

        val result = CrashDetailLoader.stackTraceFrom(event)

        assertEquals("Unknown\npackage=pkg id=1", result)
    }

    @Test
    fun `stackTraceFrom falls back when stackTrace is only whitespace`() {
        val event = event(
            stackTrace = "   \n  ",
            exceptionClass = "java.io.IOException",
            message = "disk full",
            packageName = "com.test",
            id = "id-ws",
        )

        val result = CrashDetailLoader.stackTraceFrom(event)

        assertEquals("java.io.IOException: disk full\npackage=com.test id=id-ws", result)
    }

    // -- titleFromStackTrace ---------------------------------------------------

    @Test
    fun `titleFromStackTrace extracts short exception name from multi-line trace`() {
        val trace = "java.lang.NullPointerException: Something is null\n\tat com.example.Foo.bar(Foo.java:42)"

        assertEquals("NullPointerException", CrashDetailLoader.titleFromStackTrace(trace))
    }

    @Test
    fun `titleFromStackTrace returns null for empty string`() {
        assertNull(CrashDetailLoader.titleFromStackTrace(""))
    }

    @Test
    fun `titleFromStackTrace returns null for blank-only string`() {
        assertNull(CrashDetailLoader.titleFromStackTrace("   \n  "))
    }

    @Test
    fun `titleFromStackTrace returns full first line when no colon present`() {
        val trace = "com.example.CustomException\n\tat ..."

        assertEquals("CustomException", CrashDetailLoader.titleFromStackTrace(trace))
    }

    @Test
    fun `titleFromStackTrace handles single-line trace without colon`() {
        val trace = "RuntimeException"

        assertEquals("RuntimeException", CrashDetailLoader.titleFromStackTrace(trace))
    }

    @Test
    fun `titleFromStackTrace handles unqualified exception class`() {
        val trace = "MyException: something happened"

        assertEquals("MyException", CrashDetailLoader.titleFromStackTrace(trace))
    }

    // -- formatFallback edge cases ---------------------------------------------

    @Test
    fun `formatFallback omits colon part when message is null`() {
        val event = event(
            exceptionClass = "java.lang.RuntimeException",
            message = null,
            packageName = "com.pkg",
            id = "id-null",
        )

        val result = CrashDetailLoader.stackTraceFrom(event)

        assertEquals("java.lang.RuntimeException\npackage=com.pkg id=id-null", result)
    }

    @Test
    fun `formatFallback includes colon and message when message is non-null`() {
        val event = event(
            exceptionClass = "java.lang.RuntimeException",
            message = "oops",
            packageName = "com.pkg",
            id = "id-msg",
        )

        val result = CrashDetailLoader.stackTraceFrom(event)

        assertEquals("java.lang.RuntimeException: oops\npackage=com.pkg id=id-msg", result)
    }

    @Test
    fun `formatFallback handles empty message as non-null`() {
        val event = event(
            exceptionClass = "Ex",
            message = "",
            packageName = "pkg",
            id = "1",
        )

        val result = CrashDetailLoader.stackTraceFrom(event)

        // empty string is non-null, so ': ' is appended
        assertEquals("Ex: \npackage=pkg id=1", result)
    }

    // -- helper ----------------------------------------------------------------

    private fun event(
        id: String = "evt-1",
        packageName: String = "com.example",
        exceptionClass: String = "java.lang.Exception",
        message: String? = null,
        stackTrace: String = "",
    ) = CrashEvent(
        id = id,
        timestampMs = 0L,
        packageName = packageName,
        appLabel = null,
        processName = null,
        exceptionClass = exceptionClass,
        message = message,
        stackTrace = stackTrace,
        source = null,
    )
}
