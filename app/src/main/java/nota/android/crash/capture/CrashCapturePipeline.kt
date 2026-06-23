package nota.android.crash.capture

import android.app.Application
import android.content.pm.ApplicationInfo
import de.robv.android.xposed.XposedBridge
import nota.android.crash.feedback.CrashFeedbackFacade
import nota.android.crash.log.CrashEventBuilder
import nota.android.crash.log.CrashLogCoordinator
import nota.android.crash.log.hookSafeLog
import androidx.annotation.VisibleForTesting
import nota.android.crash.xp.ScopeDecision

/**
 * Hook-side crash capture entry (ADR-011).
 * Observation and feedback paths use independent failure domains.
 */
object CrashCapturePipeline {

    /** Test-only: replace coordinator call; null uses real [CrashLogCoordinator]. */
    @JvmStatic
    @VisibleForTesting
    internal var testCoordinator: CrashLogCoordinator? = null

    /** Test-only: replace feedback call; null uses real [CrashFeedbackFacade]. */
    @JvmStatic
    @VisibleForTesting
    internal var testFeedback: CrashFeedbackFacade? = null

    @JvmStatic
    fun onException(
        application: Application,
        packageName: String,
        appInfo: ApplicationInfo,
        throwable: Throwable,
        source: String,
        decision: ScopeDecision,
    ) {
        try {
            var appLabel: String? = null
            try {
                appLabel = appInfo.loadLabel(application.packageManager).toString()
            } catch (e: Throwable) {
                hookSafeLog("CrashCapturePipeline", "Failed to load app label", e)
            }

            val processName = CrashEventBuilder.resolveProcessName(application, packageName)
            val event = CrashEventBuilder.build(
                packageName = packageName,
                appLabel = appLabel,
                processName = processName,
                throwable = throwable,
                source = source,
            )

            // Observation and feedback are independent failure domains (ADR-011).
            // Per-app crashLogEnabled from ScopeDecision gates log writes.
            if (decision.crashLogEnabled) {
                try {
                    val coordinator = testCoordinator ?: CrashLogCoordinator
                    if (decision.shouldIntercept) {
                        coordinator.logAsync(application, event)
                    } else {
                        coordinator.logSync(application, event)
                    }
                } catch (t: Throwable) {
                    XposedBridge.log(t)
                }
            }

            XposedBridge.log(throwable)

            try {
                (testFeedback ?: CrashFeedbackFacade).show(
                    application,
                    packageName,
                    appInfo,
                    throwable,
                    event.id,
                    decision.showNotify,
                )
            } catch (t: Throwable) {
                XposedBridge.log(t)
            }
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }
}
