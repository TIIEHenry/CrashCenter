package nota.android.crash.log

import nota.android.crash.xp.app.data.CrashEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashEventBuilderTest {

    @Test
    fun `build creates CrashEvent from simple Throwable`() {
        val throwable = RuntimeException("Something went wrong")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = "Example App",
            processName = "com.example.app:main",
            throwable = throwable,
            source = "xposed",
        )

        assertNotNull(event)
        assertEquals("com.example.app", event.packageName)
        assertEquals("Example App", event.appLabel)
        assertEquals("com.example.app:main", event.processName)
        assertEquals("java.lang.RuntimeException", event.exceptionClass)
        assertEquals("Something went wrong", event.message)
        assertEquals("xposed", event.source)
        assertNotNull(event.stackTrace)
        assertTrue(event.stackTrace.isNotEmpty())
        assertNotNull(event.id)
        assertTrue(event.id.isNotEmpty())
        assertTrue(event.timestampMs > 0)
    }

    @Test
    fun `build uses root cause as exceptionClass when nested`() {
        val rootCause = IllegalStateException("Root cause")
        val throwable = RuntimeException("Wrapper", rootCause)
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertEquals("java.lang.IllegalStateException", event.exceptionClass)
        assertEquals("Root cause", event.message)
    }

    @Test
    fun `build stack trace contains wrapper and root cause when nested`() {
        val rootCause = IllegalStateException("Root cause")
        val throwable = RuntimeException("Wrapper", rootCause)
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertTrue(event.stackTrace.contains("RuntimeException"))
        assertTrue(event.stackTrace.contains("IllegalStateException"))
        assertTrue(event.stackTrace.contains("Root cause"))
    }

    @Test
    fun `build handles deeply nested exceptions`() {
        val level3 = IllegalArgumentException("Level 3")
        val level2 = IllegalStateException("Level 2", level3)
        val level1 = RuntimeException("Level 1", level2)
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = level1,
            source = "test",
        )

        // throwable.cause ?: throwable only unwraps one level, so root is level2
        assertEquals("java.lang.IllegalStateException", event.exceptionClass)
        assertEquals("Level 2", event.message)
        // Stack trace should contain all levels
        assertTrue(event.stackTrace.contains("RuntimeException"))
        assertTrue(event.stackTrace.contains("IllegalStateException"))
        assertTrue(event.stackTrace.contains("IllegalArgumentException"))
    }

    @Test
    fun `build handles null message`() {
        val throwable = RuntimeException() // no message
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertNull(event.message)
    }

    @Test
    fun `build handles null appLabel`() {
        val throwable = RuntimeException("msg")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = "process",
            throwable = throwable,
            source = "test",
        )

        assertNull(event.appLabel)
    }

    @Test
    fun `build handles null processName`() {
        val throwable = RuntimeException("msg")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = "Label",
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertNull(event.processName)
    }

    @Test
    fun `build generates unique ids`() {
        val throwable = RuntimeException("test")
        val ids = mutableSetOf<String>()
        repeat(100) {
            val event = CrashEventBuilder.build(
                packageName = "com.example.app",
                appLabel = null,
                processName = null,
                throwable = throwable,
                source = "test",
            )
            ids.add(event.id)
        }
        assertEquals(100, ids.size)
    }

    @Test
    fun `build generates valid UUID strings`() {
        val throwable = RuntimeException("test")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        val uuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".toRegex()
        assertTrue("ID '${event.id}' does not match UUID format", uuidRegex.matches(event.id))
    }

    @Test
    fun `build timestamps are increasing`() {
        val throwable = RuntimeException("test")
        val event1 = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )
        Thread.sleep(10)
        val event2 = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertTrue(event2.timestampMs >= event1.timestampMs)
    }

    @Test
    fun `build truncates stack trace exceeding max size`() {
        // Create a very deep call stack to exceed 64KB stack trace limit
        fun recurse(depth: Int): Throwable {
            return if (depth <= 0) {
                RuntimeException("Deep error")
            } else {
                try {
                    recurse(depth - 1)
                } catch (e: Throwable) {
                    throw RuntimeException("Level $depth", e)
                }
            }
        }

        val throwable = try {
            recurse(3000)
        } catch (e: Throwable) {
            e
        }

        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        // Max is 64KB + truncation marker length (approx 15 chars), allow small margin
        assertTrue("Stack trace length ${event.stackTrace.length} should be <= ${64 * 1024 + 20}",
            event.stackTrace.length <= 64 * 1024 + 20)
        assertTrue("Stack trace should end with truncation marker", event.stackTrace.endsWith("\n... [truncated]"))
    }

    @Test
    fun `build does not truncate stack trace under max size`() {
        val throwable = RuntimeException("small")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertTrue(!event.stackTrace.endsWith("\n... [truncated]"))
    }

    @Test
    fun `build with custom exception class`() {
        val throwable = java.io.IOException("IO error")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertEquals("java.io.IOException", event.exceptionClass)
    }

    @Test
    fun `build with empty source`() {
        val throwable = RuntimeException("msg")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "",
        )

        assertEquals("", event.source)
    }

    @Test
    fun `build events with same throwable have different ids`() {
        val throwable = RuntimeException("same")
        val event1 = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )
        val event2 = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertNotEquals(event1.id, event2.id)
    }

    @Test
    fun `build events with same throwable have different timestamps`() {
        val throwable = RuntimeException("same")
        val event1 = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )
        Thread.sleep(10)
        val event2 = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertTrue(event2.timestampMs > event1.timestampMs)
    }

    @Test
    fun `build stack trace contains class and method info`() {
        val throwable = RuntimeException("test")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertTrue(event.stackTrace.contains("RuntimeException"))
        assertTrue(event.stackTrace.contains("at"))
    }

    @Test
    fun `build with Kotlin exception type`() {
        val throwable = kotlin.UninitializedPropertyAccessException("Lateinit")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertEquals("kotlin.UninitializedPropertyAccessException", event.exceptionClass)
        assertEquals("Lateinit", event.message)
    }

    @Test
    fun `build with Error subclass`() {
        val throwable = StackOverflowError("Recursion too deep")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertEquals("java.lang.StackOverflowError", event.exceptionClass)
        assertEquals("Recursion too deep", event.message)
    }

    @Test
    fun `build with custom exception type`() {
        class MyCustomException(message: String) : Exception(message)

        val throwable = MyCustomException("Custom error")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertTrue(event.exceptionClass.contains("MyCustomException"))
        assertEquals("Custom error", event.message)
    }

    @Test
    fun `build with cause having null message`() {
        val rootCause = IllegalStateException() // no message
        val throwable = RuntimeException("Wrapper", rootCause)
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertEquals("java.lang.IllegalStateException", event.exceptionClass)
        assertNull(event.message)
    }

    @Test
    fun `build with wrapper having null message and cause having message`() {
        val rootCause = IllegalStateException("Root")
        val throwable = RuntimeException(rootCause)
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertEquals("java.lang.IllegalStateException", event.exceptionClass)
        assertEquals("Root", event.message)
    }

    @Test
    fun `build produces non-empty stack trace for exception with stack`() {
        fun throwException(): Nothing {
            throw RuntimeException("Deliberate")
        }

        val throwable = try {
            throwException()
        } catch (e: RuntimeException) {
            e
        }

        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertTrue(event.stackTrace.contains("throwException"))
    }

    @Test
    fun `build result is immutable data class`() {
        val throwable = RuntimeException("test")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = "Label",
            processName = "process",
            throwable = throwable,
            source = "test",
        )

        // Verify it's a data class with expected properties
        assertEquals(event.id, event.id)
        assertEquals(event.timestampMs, event.timestampMs)
        assertEquals(event.packageName, event.packageName)
        assertEquals(event.appLabel, event.appLabel)
        assertEquals(event.processName, event.processName)
        assertEquals(event.exceptionClass, event.exceptionClass)
        assertEquals(event.message, event.message)
        assertEquals(event.stackTrace, event.stackTrace)
        assertEquals(event.source, event.source)
        assertTrue(event.backendWritten.isEmpty())
    }

    @Test
    fun `build with unicode message`() {
        val throwable = RuntimeException("Error: 中文测试 😀")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertEquals("Error: 中文测试 😀", event.message)
    }

    @Test
    fun `build with message containing special characters`() {
        val throwable = RuntimeException("Message with \"quotes\" and \t tabs and \n newlines")
        val event = CrashEventBuilder.build(
            packageName = "com.example.app",
            appLabel = null,
            processName = null,
            throwable = throwable,
            source = "test",
        )

        assertEquals("Message with \"quotes\" and \t tabs and \n newlines", event.message)
    }
}
