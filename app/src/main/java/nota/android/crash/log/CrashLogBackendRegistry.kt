package nota.android.crash.log

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import nota.android.crash.log.backend.LocalCacheBackend
import nota.android.crash.xp.PrefManager

object CrashLogBackendRegistry {

    private val hookBackends: List<CrashLogBackend> = listOf(LocalCacheBackend)

    fun enabledHookBackends(prefs: SharedPreferences): List<CrashLogBackend> {
        if (!prefs.getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)) return emptyList()
        if (!prefs.getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_LOCAL_CACHE, true)) return emptyList()
        return hookBackends
    }

    fun enabledHookBackends(context: Context): List<CrashLogBackend> {
        val prefs = XSharedPreferences(PrefManager.PACKAGE_NAME, PrefManager.PREF_NAME)
        prefs.reload()
        return enabledHookBackends(prefs as SharedPreferences)
    }

    /** @deprecated Use [enabledHookBackends]. */
    fun enabledHookPhase2Backends(prefs: SharedPreferences): List<CrashLogBackend> =
        enabledHookBackends(prefs)

    /** @deprecated Use [enabledHookBackends]. */
    fun enabledHookPhase2Backends(context: Context): List<CrashLogBackend> =
        enabledHookBackends(context)

    /** Module-side registry empty after ADR-024 (root I/O via repository). */
    fun enabledModuleBackends(prefs: SharedPreferences): List<CrashLogBackend> = emptyList()

    fun enabledModuleBackends(context: Context): List<CrashLogBackend> = emptyList()
}
