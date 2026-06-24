package nota.android.crash.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLogBackendTest {

    @Test
    fun `BackendId has ADR-024 values`() {
        val ids = BackendId.entries
        assertEquals(2, ids.size)
        assertTrue(ids.contains(BackendId.LOCAL_CACHE))
        assertTrue(ids.contains(BackendId.ROOT_FS))
    }

    @Test
    fun `BackendId wireName values are correct`() {
        assertEquals("local_cache", BackendId.LOCAL_CACHE.wireName)
        assertEquals("root_fsm", BackendId.ROOT_FS.wireName)
    }

    @Test
    fun `wire names are unique`() {
        val names = BackendId.entries.map { it.wireName }
        assertEquals(names.size, names.toSet().size)
    }
}
