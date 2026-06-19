package nota.android.crash.capture

import android.app.Application
import android.content.pm.ApplicationInfo
import de.robv.android.xposed.XposedBridge
import nota.android.crash.feedback.CrashFeedbackFacade
import nota.android.crash.log.CrashLogCoordinator
import nota.android.crash.xp.ScopeDecision

/**
 * Hook-side crash capture entry (ADR-011).
 * Observation and feedback paths use independent failure domains.
 */
object CrashCapturePipeline {

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
            } catch (_: Throwable) {
            }

            CrashLogCoordinator.logAsync(
                application,
                packageName,
                appLabel,
                throwable,
                source,
            )
            XposedBridge.log(throwable)
            CrashFeedbackFacade.show(
                application,
                packageName,
                appInfo,
                throwable,
                decision.showNotify,
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }
}
