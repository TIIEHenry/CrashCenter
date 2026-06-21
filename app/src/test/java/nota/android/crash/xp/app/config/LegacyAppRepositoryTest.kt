package nota.android.crash.xp.app.config

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import nota.android.crash.xp.PrefManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LegacyAppRepositoryTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var repository: LegacyAppRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository = LegacyAppRepository(context, prefs)
    }

    // ─── Shared preferences reading/writing ───

    @Test
    fun `readScopeMode returns default false`() {
        assertFalse(repository.readScopeMode())
    }

    @Test
    fun `readScopeMode returns stored value`() {
        repository.setScopeMode(true)
        assertTrue(repository.readScopeMode())
    }

    @Test
    fun `readHandleSystem returns default false`() {
        assertFalse(repository.readHandleSystem())
    }

    @Test
    fun `readHandleSystem returns stored value`() {
        repository.setHandleSystem(true)
        assertTrue(repository.readHandleSystem())
    }

    @Test
    fun `readShowSystemUi returns default false`() {
        assertFalse(repository.readShowSystemUi())
    }

    @Test
    fun `readShowSystemUi returns stored value`() {
        repository.setShowSystemUi(true)
        assertTrue(repository.readShowSystemUi())
    }

    @Test
    fun `setScopeMode persists to shared preferences`() {
        repository.setScopeMode(true)
        val stored = prefs.getBoolean(PrefManager.PREF_SCOPE_MODE, false)
        assertTrue(stored)
    }

    @Test
    fun `setHandleSystem persists to shared preferences`() {
        repository.setHandleSystem(true)
        val stored = prefs.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false)
        assertTrue(stored)
    }

    @Test
    fun `setShowSystemUi persists to shared preferences`() {
        repository.setShowSystemUi(true)
        val stored = prefs.getBoolean(PrefManager.PREF_SHOW_SYSTEM_UI, false)
        assertTrue(stored)
    }

    @Test
    fun `toggle scope mode back and forth`() {
        assertFalse(repository.readScopeMode())
        repository.setScopeMode(true)
        assertTrue(repository.readScopeMode())
        repository.setScopeMode(false)
        assertFalse(repository.readScopeMode())
    }

    // ─── persistHookStates ───

    @Test
    fun `persistHookStates saves disabled packages`() {
        val apps = listOf(
            fakeAppItem("com.example.a", hookEnabled = true),
            fakeAppItem("com.example.b", hookEnabled = false),
            fakeAppItem("com.example.c", hookEnabled = false),
        )
        repository.persistHookStates(apps)

        val disabled = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null)
        assertNotNull(disabled)
        assertEquals(2, disabled!!.size)
        assertTrue(disabled.contains("com.example.b"))
        assertTrue(disabled.contains("com.example.c"))
        assertFalse(disabled.contains("com.example.a"))
    }

    @Test
    fun `persistHookStates with all enabled saves empty set`() {
        val apps = listOf(
            fakeAppItem("com.example.a", hookEnabled = true),
            fakeAppItem("com.example.b", hookEnabled = true),
        )
        repository.persistHookStates(apps)

        val disabled = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null)
        assertNotNull(disabled)
        assertTrue(disabled!!.isEmpty())
    }

    @Test
    fun `persistHookStates with all disabled saves all packages`() {
        val apps = listOf(
            fakeAppItem("com.example.a", hookEnabled = false),
            fakeAppItem("com.example.b", hookEnabled = false),
        )
        repository.persistHookStates(apps)

        val disabled = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null)
        assertNotNull(disabled)
        assertEquals(2, disabled!!.size)
    }

    @Test
    fun `persistHookStates with empty list saves empty set`() {
        repository.persistHookStates(emptyList())
        val disabled = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null)
        assertNotNull(disabled)
        assertTrue(disabled!!.isEmpty())
    }

    @Test
    fun `persistHookStates filters out self package`() {
        val apps = listOf(
            fakeAppItem(PrefManager.ITSELF, hookEnabled = false),
            fakeAppItem("com.example.a", hookEnabled = false),
        )
        repository.persistHookStates(apps)

        val disabled = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null)
        assertNotNull(disabled)
        assertTrue(disabled!!.contains("com.example.a"))
        assertTrue(disabled.contains(PrefManager.ITSELF))
    }

    // ─── Helpers ───

    private fun fakeAppItem(
        packageName: String,
        hookEnabled: Boolean = true,
        isSystem: Boolean = false,
    ): AppItem = AppItem(
        label = packageName,
        appInfo = ApplicationInfo().apply {
            this.packageName = packageName
            flags = if (isSystem) ApplicationInfo.FLAG_SYSTEM else 0
        },
        hookEnabled = hookEnabled,
        packageName = packageName,
        isSystem = isSystem,
        updateTime = 0L,
        installTime = 0L,
    )
}
