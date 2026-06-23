package nota.android.crash.log

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import nota.android.crash.log.backend.TargetRelayBackend
import nota.android.crash.xp.PrefManager

/**
 * Hook-side crash log coordinator (4B-α).
 * Phase 2 parallel: Provider + DirectFs + TargetRelay + RootSuBackend
 * all run concurrently with a shared timeout.
 *
 * Sequential validation/formatting on a single dispatcher, then parallel backend
 * writes with timeout via structured coroutines.
 */
object CrashLogCoordinator {

    private const val PARALLEL_TIMEOUT_MS = 2000L
    private const val OBSERVE_SYNC_TIMEOUT_MS = 500L

    private val coordinatorScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO.limitedParallelism(1)
    )

    fun logAsync(
        hookContext: Context,
        event: nota.android.crash.common.data.CrashEvent,
    ) {
        coordinatorScope.launch {
            try {
                if (!isLoggingEnabled()) return@launch
                runPhase2Parallel(hookContext, event)
            } catch (t: Throwable) {
                hookSafeLog("CrashLogCoordinator failed: ${t.message}")
            }
        }
    }

    /**
     * Observe-mode path (ADR-023): block briefly so relay/canonical write completes before process exit.
     */
    fun logSync(
        hookContext: Context,
        event: nota.android.crash.common.data.CrashEvent,
    ) {
        try {
            if (!isLoggingEnabled()) return
            runBlocking {
                withTimeout(OBSERVE_SYNC_TIMEOUT_MS) {
                    when (TargetRelayBackend.append(hookContext, event, OBSERVE_SYNC_TIMEOUT_MS)) {
                        is AppendResult.Success -> return@withTimeout
                        is AppendResult.Failure -> runPhase2Parallel(hookContext, event)
                    }
                }
            }
        } catch (t: Throwable) {
            hookSafeLog("CrashLogCoordinator logSync failed: ${t.message}")
        }
    }

    @androidx.annotation.VisibleForTesting
    internal suspend fun runPhase2Parallel(
        hookContext: Context,
        event: nota.android.crash.common.data.CrashEvent,
    ) {
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(hookContext)
        runPhase2Parallel(hookContext, event, backends)
    }

    @androidx.annotation.VisibleForTesting
    internal suspend fun runPhase2Parallel(
        hookContext: Context,
        event: nota.android.crash.common.data.CrashEvent,
        backends: List<CrashLogBackend>,
    ) {
        if (backends.isEmpty()) return

        val written = mutableListOf<String>()

        try {
            withTimeout(PARALLEL_TIMEOUT_MS) {
                val deferreds = backends.map { backend ->
                    async(Dispatchers.IO) {
                        try {
                            when (backend.append(hookContext, event, PARALLEL_TIMEOUT_MS)) {
                                is AppendResult.Success -> backend.id.wireName
                                is AppendResult.Failure -> null
                            }
                        } catch (t: Throwable) {
                            hookSafeLog("CrashLog ${backend.id.wireName} failed: ${t.message}")
                            null
                        }
                    }
                }
                deferreds.awaitAll().filterNotNull().forEach { written.add(it) }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            // Timeout exceeded; some backends may still have succeeded
        }

        if (written.isEmpty()) {
            hookSafeLog("CrashLog: all Phase 2 backends failed for ${event.id}")
        }
    }

    fun shutdown() {
        coordinatorScope.cancel()
    }

    @androidx.annotation.VisibleForTesting
    internal fun isLoggingEnabled(): Boolean {
        val prefs = XSharedPreferences(PrefManager.PACKAGE_NAME, PrefManager.PREF_NAME)
        prefs.reload()
        return prefs.getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)
    }
}
