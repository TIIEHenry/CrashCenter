package nota.android.crash.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLogBackendTest {

    // ---------- BackendId ----------

    @Test
    fun `BackendId has all expected values`() {
        val ids = BackendId.entries
        assertEquals(5, ids.size)
        assertTrue(ids.contains(BackendId.ROOT_SU))
        assertTrue(ids.contains(BackendId.ROOT_FS))
        assertTrue(ids.contains(BackendId.PROVIDER_INSERT))
        assertTrue(ids.contains(BackendId.DIRECT_FS))
        assertTrue(ids.contains(BackendId.TARGET_RELAY))
    }

    @Test
    fun `BackendId wireName values are correct`() {
        assertEquals("root_su", BackendId.ROOT_SU.wireName)
        assertEquals("root_fsm", BackendId.ROOT_FS.wireName)
        assertEquals("provider_insert", BackendId.PROVIDER_INSERT.wireName)
        assertEquals("direct_fs", BackendId.DIRECT_FS.wireName)
        assertEquals("target_relay", BackendId.TARGET_RELAY.wireName)
    }

    @Test
    fun `BackendId wireName values are distinct`() {
        val names = BackendId.entries.map { it.wireName }
        assertEquals(names.size, names.toSet().size)
    }

    // ---------- ProcessSlot ----------

    @Test
    fun `ProcessSlot has all expected values`() {
        val slots = ProcessSlot.entries
        assertEquals(2, slots.size)
        assertTrue(slots.contains(ProcessSlot.HOOK))
        assertTrue(slots.contains(ProcessSlot.MODULE))
    }

    @Test
    fun `ProcessSlot values are distinct`() {
        assertNotEquals(ProcessSlot.HOOK, ProcessSlot.MODULE)
    }

    // ---------- BackendAvailability ----------

    @Test
    fun `BackendAvailability has all expected values`() {
        val availabilities = BackendAvailability.entries
        assertEquals(3, availabilities.size)
        assertTrue(availabilities.contains(BackendAvailability.READY))
        assertTrue(availabilities.contains(BackendAvailability.MAYBE))
        assertTrue(availabilities.contains(BackendAvailability.UNAVAILABLE))
    }

    @Test
    fun `BackendAvailability values are distinct`() {
        assertNotEquals(BackendAvailability.READY, BackendAvailability.MAYBE)
        assertNotEquals(BackendAvailability.MAYBE, BackendAvailability.UNAVAILABLE)
        assertNotEquals(BackendAvailability.READY, BackendAvailability.UNAVAILABLE)
    }

    // ---------- AppendResult ----------

    @Test
    fun `AppendResult Success is a singleton`() {
        val a = AppendResult.Success
        val b = AppendResult.Success
        assertEquals(a, b)
    }

    @Test
    fun `AppendResult Failure carries reason`() {
        val failure = AppendResult.Failure("disk full")
        assertEquals("disk full", failure.reason)
    }

    @Test
    fun `AppendResult Failure instances with same reason are equal`() {
        val a = AppendResult.Failure("timeout")
        val b = AppendResult.Failure("timeout")
        assertEquals(a, b)
    }

    @Test
    fun `AppendResult Failure instances with different reasons are not equal`() {
        val a = AppendResult.Failure("timeout")
        val b = AppendResult.Failure("disk full")
        assertNotEquals(a, b)
    }

    @Test
    fun `AppendResult Success and Failure are not equal`() {
        val success = AppendResult.Success
        val failure = AppendResult.Failure("error")
        assertNotEquals(success, failure)
    }

    @Test
    fun `AppendResult Failure toString contains reason`() {
        val failure = AppendResult.Failure("permission denied")
        assertTrue(failure.toString().contains("permission denied"))
    }
}
