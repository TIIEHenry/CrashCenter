package nota.android.crash.log

import android.content.SharedPreferences
import nota.android.crash.xp.PrefManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLogBackendRegistryTest {

    // ─── default-on policy ───

    @Test
    fun `enabledHookPhase2Backends with empty prefs returns all Phase 2 backends`() {
        val prefs = FakeSharedPreferences()
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(prefs)
        val ids = backends.map { it.id }
        assertTrue(ids.contains(BackendId.PROVIDER_INSERT))
        assertTrue(ids.contains(BackendId.DIRECT_FS))
        assertTrue(ids.contains(BackendId.TARGET_RELAY))
        assertEquals(3, backends.size)
    }

    // ─── individual preference disabling ───

    @Test
    fun `enabledHookPhase2Backends disables provider when provider pref is false`() {
        val prefs = FakeSharedPreferences(
            PrefManager.PREF_CRASH_LOG_BACKEND_PROVIDER to false,
        )
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(prefs)
        val ids = backends.map { it.id }
        assertEquals(2, backends.size)
        assertTrue(ids.contains(BackendId.DIRECT_FS))
        assertTrue(ids.contains(BackendId.TARGET_RELAY))
    }

    @Test
    fun `enabledHookPhase2Backends disables direct_fs when direct_fs pref is false`() {
        val prefs = FakeSharedPreferences(
            PrefManager.PREF_CRASH_LOG_BACKEND_DIRECT_FS to false,
        )
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(prefs)
        val ids = backends.map { it.id }
        assertEquals(2, backends.size)
        assertTrue(ids.contains(BackendId.PROVIDER_INSERT))
        assertTrue(ids.contains(BackendId.TARGET_RELAY))
    }

    @Test
    fun `enabledHookPhase2Backends disables relay when relay pref is false`() {
        val prefs = FakeSharedPreferences(
            PrefManager.PREF_CRASH_LOG_BACKEND_RELAY to false,
        )
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(prefs)
        val ids = backends.map { it.id }
        assertEquals(2, backends.size)
        assertTrue(ids.contains(BackendId.PROVIDER_INSERT))
        assertTrue(ids.contains(BackendId.DIRECT_FS))
    }

    // ─── all backends disabled ───

    @Test
    fun `enabledHookPhase2Backends with all prefs false returns empty list`() {
        val prefs = FakeSharedPreferences(
            PrefManager.PREF_CRASH_LOG_BACKEND_PROVIDER to false,
            PrefManager.PREF_CRASH_LOG_BACKEND_DIRECT_FS to false,
            PrefManager.PREF_CRASH_LOG_BACKEND_RELAY to false,
        )
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(prefs)
        assertTrue(backends.isEmpty())
    }

    // ─── default-on with other prefs set ───

    @Test
    fun `enabledHookPhase2Backends pref true does not change default behavior`() {
        val prefs = FakeSharedPreferences(
            PrefManager.PREF_CRASH_LOG_BACKEND_PROVIDER to true,
            PrefManager.PREF_CRASH_LOG_BACKEND_DIRECT_FS to true,
            PrefManager.PREF_CRASH_LOG_BACKEND_RELAY to true,
        )
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(prefs)
        assertEquals(3, backends.size)
    }

    // ─── mixed enable/disable ───

    @Test
    fun `enabledHookPhase2Backends with mixed enable disable`() {
        val prefs = FakeSharedPreferences(
            PrefManager.PREF_CRASH_LOG_BACKEND_PROVIDER to true,
            PrefManager.PREF_CRASH_LOG_BACKEND_DIRECT_FS to false,
            PrefManager.PREF_CRASH_LOG_BACKEND_RELAY to true,
        )
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(prefs)
        val ids = backends.map { it.id }
        assertEquals(2, backends.size)
        assertTrue(ids.contains(BackendId.PROVIDER_INSERT))
        assertTrue(ids.contains(BackendId.TARGET_RELAY))
    }

    // ─── unknown keys in prefs do not affect result ───

    @Test
    fun `enabledHookPhase2Backends ignores unrelated preference keys`() {
        val prefs = FakeSharedPreferences(
            "unrelated_key" to false,
            "some_other_flag" to true,
        )
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(prefs)
        assertEquals(3, backends.size)
    }

    // ─── root_su is never in Phase 2 list ───

    @Test
    fun `enabledHookPhase2Backends never includes ROOT_SU backend`() {
        val prefs = FakeSharedPreferences()
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(prefs)
        val ids = backends.map { it.id }
        assertTrue(ids.none { it == BackendId.ROOT_SU })
    }

    // ─── module-side backends ───

    @Test
    fun `enabledModuleBackends with empty prefs returns RootFsBackend by default`() {
        val prefs = FakeSharedPreferences()
        val backends = CrashLogBackendRegistry.enabledModuleBackends(prefs)
        val ids = backends.map { it.id }
        assertTrue(ids.contains(BackendId.ROOT_FS))
        assertEquals(1, backends.size)
    }

    @Test
    fun `enabledModuleBackends disables root_fs when pref is false`() {
        val prefs = FakeSharedPreferences(
            PrefManager.PREF_CRASH_LOG_BACKEND_ROOT_FS to false,
        )
        val backends = CrashLogBackendRegistry.enabledModuleBackends(prefs)
        assertTrue(backends.isEmpty())
    }

    @Test
    fun `enabledModuleBackends includes root_fs when pref is true`() {
        val prefs = FakeSharedPreferences(
            PrefManager.PREF_CRASH_LOG_BACKEND_ROOT_FS to true,
        )
        val backends = CrashLogBackendRegistry.enabledModuleBackends(prefs)
        assertEquals(1, backends.size)
        assertEquals(BackendId.ROOT_FS, backends[0].id)
    }

    @Test
    fun `enabledModuleBackends never includes hook-side backends`() {
        val prefs = FakeSharedPreferences()
        val backends = CrashLogBackendRegistry.enabledModuleBackends(prefs)
        val ids = backends.map { it.id }
        assertTrue(ids.none { it == BackendId.PROVIDER_INSERT })
        assertTrue(ids.none { it == BackendId.DIRECT_FS })
        assertTrue(ids.none { it == BackendId.TARGET_RELAY })
    }

    // ─── Fake SharedPreferences ───

    private class FakeSharedPreferences(
        vararg initial: Pair<String, Any>,
    ) : SharedPreferences {

        private val store = mutableMapOf<String, Any>(*initial)

        override fun getAll(): Map<String, *> = store.toMap()

        override fun getString(key: String, defValue: String?): String? =
            store[key] as? String ?: defValue

        override fun getStringSet(key: String, defValue: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (store[key] as? MutableSet<String>) ?: defValue

        override fun getInt(key: String, defValue: Int): Int =
            (store[key] as? Int) ?: defValue

        override fun getLong(key: String, defValue: Long): Long =
            (store[key] as? Long) ?: defValue

        override fun getFloat(key: String, defValue: Float): Float =
            (store[key] as? Float) ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean =
            (store[key] as? Boolean) ?: defValue

        override fun contains(key: String): Boolean = store.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(store)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        private class FakeEditor(
            private val store: MutableMap<String, Any>,
        ) : SharedPreferences.Editor {

            private val pending = mutableMapOf<String, Any?>()

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun putStringSet(
                key: String,
                values: MutableSet<String>?,
            ): SharedPreferences.Editor {
                pending[key] = values; return this
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                pending[key] = null; return this
            }

            override fun clear(): SharedPreferences.Editor {
                pending.clear(); store.clear(); return this
            }

            override fun commit(): Boolean {
                pending.forEach { (k, v) ->
                    if (v == null) store.remove(k) else store[k] = v
                }
                pending.clear()
                return true
            }

            override fun apply() {
                commit()
            }
        }
    }
}
