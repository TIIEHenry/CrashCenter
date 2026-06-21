package nota.android.crash.log

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import nota.android.crash.log.backend.DirectFsBackend
import nota.android.crash.log.backend.ProviderBackend
import nota.android.crash.log.backend.TargetRelayBackend
import nota.android.crash.xp.PrefManager

/**
 * Hook-side backend registry (Phase 2 parallel backends for 4B-α).
 * RootSuBackend registration deferred to 4B-β.
 */
object CrashLogBackendRegistry {

    private val hookPhase2Backends: List<CrashLogBackend> = listOf(
        ProviderBackend,
        DirectFsBackend,
        TargetRelayBackend,
    )

    fun enabledHookPhase2Backends(prefs: SharedPreferences): List<CrashLogBackend> {
        return hookPhase2Backends.filter { backend ->
            when (backend.id) {
                BackendId.PROVIDER_INSERT ->
                    prefs.getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_PROVIDER, true)
                BackendId.DIRECT_FS ->
                    prefs.getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_DIRECT_FS, true)
                BackendId.TARGET_RELAY ->
                    prefs.getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_RELAY, true)
                else -> false
            }
        }
    }

    fun enabledHookPhase2Backends(context: Context): List<CrashLogBackend> {
        val prefs = XSharedPreferences(PrefManager.PACKAGE_NAME, PrefManager.PREF_NAME)
        prefs.reload()
        return enabledHookPhase2Backends(prefs as SharedPreferences)
    }
}
