package nota.android.crash.xp

import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage
import nota.android.crash.xp.app.config.PackageInfoLoader

object ScopePolicy {

    private val IGNORED_PACKAGES = setOf(
        "android",
        "de.robv.android.xposed.installer",
        "org.meowcat.edxposed.manager",
        "org.lsposed.manager",
    )

    @JvmStatic
    fun evaluate(
        xsp: XSharedPreferences,
        lpparam: XC_LoadPackage.LoadPackageParam,
    ): ScopeDecision {
        if (lpparam.appInfo == null) {
            return noInstall()
        }
        return evaluate(
            packageName = lpparam.packageName,
            isSystemApp = PackageInfoLoader.isSystemApp(lpparam.appInfo),
            handleSystem = xsp.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false),
            interceptEnabled = xsp.contains(PrefManager.PREF_MANAGED_PACKAGES) &&
                xsp.getStringSet(PrefManager.PREF_MANAGED_PACKAGES, emptySet())!!
                    .contains(lpparam.packageName),
        )
    }

    internal fun evaluate(
        packageName: String,
        isSystemApp: Boolean,
        handleSystem: Boolean,
        interceptEnabled: Boolean,
    ): ScopeDecision {
        if (packageName in IGNORED_PACKAGES) {
            return noInstall()
        }
        if (!passesSystemFilter(isSystemApp, handleSystem)) {
            return noInstall()
        }
        return if (interceptEnabled) {
            intercept(showNotify = true, crashLogEnabled = true)
        } else {
            observeOnly(crashLogEnabled = true)
        }
    }

    private fun passesSystemFilter(
        isSystemApp: Boolean,
        handleSystem: Boolean,
    ): Boolean = !isSystemApp || handleSystem

    private fun noInstall(): ScopeDecision =
        ScopeDecision(
            shouldInstall = false,
            shouldIntercept = false,
            showNotify = false,
            crashLogEnabled = false,
        )

    private fun observeOnly(crashLogEnabled: Boolean = true): ScopeDecision =
        ScopeDecision(
            shouldInstall = true,
            shouldIntercept = false,
            showNotify = false,
            crashLogEnabled = crashLogEnabled,
        )

    private fun intercept(showNotify: Boolean, crashLogEnabled: Boolean): ScopeDecision =
        ScopeDecision(
            shouldInstall = true,
            shouldIntercept = true,
            showNotify = showNotify,
            crashLogEnabled = crashLogEnabled,
        )
}
