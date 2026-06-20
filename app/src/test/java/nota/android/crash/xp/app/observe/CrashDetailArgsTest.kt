package nota.android.crash.xp.app.observe

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashDetailArgsTest {

    @Test
    fun `fromBundle with crash_id returns FromId`() {
        val bundle = Bundle().apply {
            putString("crash_id", "crash-123")
        }
        val result = CrashDetailArgs.fromBundle(bundle)
        assertTrue(result is CrashDetailArgs.FromId)
        assertEquals("crash-123", (result as CrashDetailArgs.FromId).crashId)
    }

    @Test
    fun `fromBundle with Exception extra returns FromStackTrace`() {
        val bundle = Bundle().apply {
            putString("stack_trace", "java.lang.NullPointerException at Foo.bar")
        }
        val result = CrashDetailArgs.fromBundle(bundle)
        assertTrue(result is CrashDetailArgs.FromStackTrace)
        assertEquals("java.lang.NullPointerException at Foo.bar", (result as CrashDetailArgs.FromStackTrace).stackTrace)
        assertEquals(null, result.title)
    }

    @Test
    fun `fromBundle with Exception extra and title returns FromStackTrace with title`() {
        val bundle = Bundle().apply {
            putString("stack_trace", "trace content")
            putString("title", "My Title")
        }
        val result = CrashDetailArgs.fromBundle(bundle)
        assertTrue(result is CrashDetailArgs.FromStackTrace)
        assertEquals("trace content", (result as CrashDetailArgs.FromStackTrace).stackTrace)
        assertEquals("My Title", result.title)
    }

    @Test
    fun `fromBundle with blank stack_trace falls back to FromId`() {
        val bundle = Bundle().apply {
            putString("crash_id", "crash-456")
            putString("stack_trace", "   ")
        }
        val result = CrashDetailArgs.fromBundle(bundle)
        assertTrue(result is CrashDetailArgs.FromId)
        assertEquals("crash-456", (result as CrashDetailArgs.FromId).crashId)
    }

    @Test
    fun `fromBundle with empty stack_trace falls back to FromId`() {
        val bundle = Bundle().apply {
            putString("crash_id", "crash-789")
            putString("stack_trace", "")
        }
        val result = CrashDetailArgs.fromBundle(bundle)
        assertTrue(result is CrashDetailArgs.FromId)
        assertEquals("crash-789", (result as CrashDetailArgs.FromId).crashId)
    }

    @Test
    fun `fromBundle with null stack_trace falls back to FromId`() {
        val bundle = Bundle().apply {
            putString("crash_id", "crash-abc")
        }
        val result = CrashDetailArgs.fromBundle(bundle)
        assertTrue(result is CrashDetailArgs.FromId)
        assertEquals("crash-abc", (result as CrashDetailArgs.FromId).crashId)
    }

    @Test
    fun `fromBundle with missing crash_id returns FromId with empty string`() {
        val bundle = Bundle()
        val result = CrashDetailArgs.fromBundle(bundle)
        assertTrue(result is CrashDetailArgs.FromId)
        assertEquals("", (result as CrashDetailArgs.FromId).crashId)
    }

    @Test
    fun `fromBundle prefers stack_trace over crash_id when both present and non-blank`() {
        val bundle = Bundle().apply {
            putString("crash_id", "crash-999")
            putString("stack_trace", "valid trace")
            putString("title", "Legacy Title")
        }
        val result = CrashDetailArgs.fromBundle(bundle)
        assertTrue(result is CrashDetailArgs.FromStackTrace)
        assertEquals("valid trace", (result as CrashDetailArgs.FromStackTrace).stackTrace)
        assertEquals("Legacy Title", result.title)
    }

    @Test
    fun `toBundle round-trip for FromId`() {
        val original = CrashDetailArgs.FromId("round-trip-id")
        val bundle = original.toBundle()
        val restored = CrashDetailArgs.fromBundle(bundle)
        assertTrue(restored is CrashDetailArgs.FromId)
        assertEquals("round-trip-id", (restored as CrashDetailArgs.FromId).crashId)
    }

    @Test
    fun `toBundle round-trip for FromStackTrace without title`() {
        val original = CrashDetailArgs.FromStackTrace("stack content")
        val bundle = original.toBundle()
        val restored = CrashDetailArgs.fromBundle(bundle)
        assertTrue(restored is CrashDetailArgs.FromStackTrace)
        assertEquals("stack content", (restored as CrashDetailArgs.FromStackTrace).stackTrace)
        assertEquals(null, restored.title)
    }

    @Test
    fun `toBundle round-trip for FromStackTrace with title`() {
        val original = CrashDetailArgs.FromStackTrace("stack content", "Title")
        val bundle = original.toBundle()
        val restored = CrashDetailArgs.fromBundle(bundle)
        assertTrue(restored is CrashDetailArgs.FromStackTrace)
        assertEquals("stack content", (restored as CrashDetailArgs.FromStackTrace).stackTrace)
        assertEquals("Title", restored.title)
    }

    @Test
    fun `fromBundle with blank crash_id and no stack_trace returns FromId with empty string`() {
        val bundle = Bundle().apply {
            putString("crash_id", "   ")
        }
        val result = CrashDetailArgs.fromBundle(bundle)
        assertTrue(result is CrashDetailArgs.FromId)
        assertEquals("   ", (result as CrashDetailArgs.FromId).crashId)
    }
}
