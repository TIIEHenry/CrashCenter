package nota.android.crash.log

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import nota.android.crash.log.backend.DirectFsBackend
import nota.android.crash.log.backend.ProviderBackend
import nota.android.crash.log.backend.RelayMergeBackend
import nota.android.crash.log.backend.RootFsBackend
import nota.android.crash.log.backend.RootSuBackend
import nota.android.crash.log.backend.TargetRelayBackend
import nota.android.crash.xp.PrefManager

/**
 * Backend registry for both hook-side (Phase 2) and module-side backends.
 * Hook-side: Provider + DirectFs + TargetRelay (4B-α).
 * Module-side: RootFsBackend (4B-β ingest path).
 */
object CrashLogBackendRegistry {

    private val hookPhase2Backends: List<CrashLogBackend> = listOf(
        RootSuBackend,
        ProviderBackend,
        DirectFsBackend,
        TargetRelayBackend,
    )

    private val moduleBackends: List<CrashLogBackend> = listOf(
        RootFsBackend,
        RelayMergeBackend,
    )

    fun enabledHookPhase2Backends(prefs: SharedPreferences): List<CrashLogBackend> {
        return hookPhase2Backends.filter { backend ->
            when (backend.id) {
                BackendId.ROOT_SU ->
                    prefs.getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_ROOT_SU, true)
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

    /**
     * Returns module-side backends whose pref toggle is enabled.
     * Used by CrashLogIngestCoordinator (4B-beta) in the module process.
     */
    fun enabledModuleBackends(prefs: SharedPreferences): List<CrashLogBackend> {
        return moduleBackends.filter { backend ->
            when (backend.id) {
                BackendId.ROOT_FS ->
                    prefs.getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_ROOT_FS, true)
                BackendId.RELAY_MERGE ->
                    prefs.getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_RELAY_MERGE, true)
                else -> false
            }
        }
    }

    fun enabledModuleBackends(context: Context): List<CrashLogBackend> {
        return enabledModuleBackends(prefs(context))
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
}
