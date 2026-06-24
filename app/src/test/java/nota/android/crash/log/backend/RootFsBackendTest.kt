package nota.android.crash.log.backend

import android.content.Context
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendId
import nota.android.crash.log.ProcessSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RootFsBackendTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val sampleEvent = CrashEvent(id = "fs-1", packageName = "com.example.app")

    @Test
    fun `backend metadata matches architecture`() {
        assertEquals(BackendId.ROOT_FS, RootFsBackend.id)
        assertEquals(0, RootFsBackend.tier)
        assertEquals(ProcessSlot.MODULE, RootFsBackend.runsOn)
    }

    @Test
    fun `append is read-only and returns failure`() {
        val result = RootFsBackend.append(context, sampleEvent, deadlineMs = 5000L)
        assertTrue(result is AppendResult.Failure)
    }
}
