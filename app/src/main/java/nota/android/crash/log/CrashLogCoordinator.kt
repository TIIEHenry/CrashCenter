package nota.android.crash.log

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.backend.LocalCacheBackend
import nota.android.crash.xp.PrefManager

/** Hook-side crash log coordinator (ADR-024): local cache append only. */
object CrashLogCoordinator {

    private const val OBSERVE_SYNC_TIMEOUT_MS = 500L

    private val coordinatorScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO.limitedParallelism(1),
    )

    fun logAsync(hookContext: Context, event: CrashEvent) {
        coordinatorScope.launch {
            try {
                if (!isLoggingEnabled()) return@launch
                LocalCacheBackend.append(hookContext, event, 2000L)
            } catch (t: Throwable) {
                hookSafeLog("CrashLogCoordinator failed: ${t.message}")
            }
        }
    }

    /** Observe-mode: sync write to local cache before process exit (ADR-023). */
    fun logSync(hookContext: Context, event: CrashEvent) {
        try {
            if (!isLoggingEnabled()) return
            runBlocking {
                withTimeout(OBSERVE_SYNC_TIMEOUT_MS) {
                    LocalCacheBackend.append(hookContext, event, OBSERVE_SYNC_TIMEOUT_MS)
                }
            }
        } catch (t: Throwable) {
            hookSafeLog("CrashLogCoordinator logSync failed: ${t.message}")
        }
    }

    fun shutdown() {
        coordinatorScope.cancel()
    }

    internal fun isLoggingEnabled(): Boolean {
        val prefs = XSharedPreferences(PrefManager.PACKAGE_NAME, PrefManager.PREF_NAME)
        prefs.reload()
        if (!prefs.getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)) return false
        return prefs.getBoolean(PrefManager.PREF_CRASH_LOG_BACKEND_LOCAL_CACHE, true)
    }
}
