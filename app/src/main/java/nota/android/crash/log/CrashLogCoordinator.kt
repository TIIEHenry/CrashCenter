package nota.android.crash.log

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import nota.android.crash.xp.PrefManager
import java.util.concurrent.Executors

/**
 * Hook-side crash log coordinator (4B scaffold).
 * Async single-backend write; multi-backend orchestration deferred to 4B-α.
 */
object CrashLogCoordinator {

    private val executor = Executors.newSingleThreadExecutor()

    @JvmStatic
    fun logAsync(
        hookContext: Context,
        packageName: String,
        appLabel: String?,
        throwable: Throwable,
        source: String,
    ) {
        executor.execute {
            if (!isLoggingEnabled()) return@execute
            val processName = CrashEventBuilder.resolveProcessName(hookContext, packageName)
            val event = CrashEventBuilder.build(
                packageName = packageName,
                appLabel = appLabel,
                processName = processName,
                throwable = throwable,
                source = source,
            )
            DirectFsCrashLogWriter.appendSilent(hookContext, event)
        }
    }

    private fun isLoggingEnabled(): Boolean {
        val prefs = XSharedPreferences(PrefManager.PACKAGE_NAME, PrefManager.PREF_NAME)
        prefs.reload()
        return prefs.getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)
    }
}
