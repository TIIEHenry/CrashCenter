package nota.android.crash.log

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import nota.android.crash.xp.PrefManager
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Hook-side crash log coordinator (4B-α).
 * Phase 2 parallel: Provider + DirectFs + TargetRelay (RootSu deferred to 4B-β).
 */
object CrashLogCoordinator {

    private const val PARALLEL_TIMEOUT_MS = 2000L

    private val executor = Executors.newSingleThreadExecutor()

    fun logAsync(
        hookContext: Context,
        packageName: String,
        appLabel: String?,
        throwable: Throwable,
        source: String,
    ) {
        executor.execute {
            try {
                if (!isLoggingEnabled()) return@execute
                val processName = CrashEventBuilder.resolveProcessName(hookContext, packageName)
                val event = CrashEventBuilder.build(
                    packageName = packageName,
                    appLabel = appLabel,
                    processName = processName,
                    throwable = throwable,
                    source = source,
                )
                runPhase2Parallel(hookContext, event)
            } catch (t: Throwable) {
                XposedBridge.log("CrashLogCoordinator failed: ${t.message}")
            }
        }
    }

    private fun runPhase2Parallel(hookContext: Context, event: nota.android.crash.xp.app.data.CrashEvent) {
        val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(hookContext)
        if (backends.isEmpty()) return

        val written = Collections.synchronizedList(mutableListOf<String>())
        val latch = CountDownLatch(backends.size)
        val deadlineMs = PARALLEL_TIMEOUT_MS

        for (backend in backends) {
            Thread {
                try {
                    when (backend.append(hookContext, event, deadlineMs)) {
                        is AppendResult.Success -> written.add(backend.id.wireName)
                        is AppendResult.Failure -> Unit
                    }
                } catch (t: Throwable) {
                    XposedBridge.log("CrashLog ${backend.id.wireName} failed: ${t.message}")
                } finally {
                    latch.countDown()
                }
            }.start()
        }

        latch.await(PARALLEL_TIMEOUT_MS, TimeUnit.MILLISECONDS)

        if (written.isEmpty()) {
            XposedBridge.log("CrashLog: all Phase 2 backends failed for ${event.id}")
        }
    }

    private fun isLoggingEnabled(): Boolean {
        val prefs = XSharedPreferences(PrefManager.PACKAGE_NAME, PrefManager.PREF_NAME)
        prefs.reload()
        return prefs.getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)
    }
}
