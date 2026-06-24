package nota.android.crash.log

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import nota.android.crash.log.backend.LocalCacheBackend
import nota.android.crash.xp.PrefManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashLogBackendRegistryTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = mock()
        doReturn(true).`when`(prefs).getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)
        doReturn(true).`when`(prefs).getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_LOCAL_CACHE, true)
    }

    @Test
    fun `enabled hook backends returns LocalCache when enabled`() {
        val backends = CrashLogBackendRegistry.enabledHookBackends(prefs)
        assertEquals(1, backends.size)
        assertEquals(LocalCacheBackend, backends[0])
    }

    @Test
    fun `disabled crash log returns empty`() {
        whenever(prefs.getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)).thenReturn(false)
        assertTrue(CrashLogBackendRegistry.enabledHookBackends(prefs).isEmpty())
    }

    @Test
    fun `disabled local cache returns empty`() {
        whenever(prefs.getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_LOCAL_CACHE, true)).thenReturn(false)
        assertTrue(CrashLogBackendRegistry.enabledHookBackends(prefs).isEmpty())
    }

    @Test
    fun `module backends empty after ADR-024`() {
        assertTrue(CrashLogBackendRegistry.enabledModuleBackends(prefs).isEmpty())
        assertTrue(CrashLogBackendRegistry.enabledModuleBackends(context).isEmpty())
    }
}
