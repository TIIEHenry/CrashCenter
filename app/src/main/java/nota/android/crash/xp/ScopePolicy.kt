package nota.android.crash.xp

import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage
import nota.android.crash.xp.app.config.AppInterventionProfile
import nota.android.crash.xp.app.config.InterventionRule
import nota.android.crash.xp.app.config.InterventionRulesCodec
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
        val isSystemApp = PackageInfoLoader.isSystemApp(lpparam.appInfo)
        return evaluate(
            packageName = lpparam.packageName,
            isSystemApp = isSystemApp,
            scopeMode = xsp.getBoolean(PrefManager.PREF_SCOPE_MODE, false),
            handleSystem = xsp.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false),
            packageListDisabled = xsp.getStringSet(PrefManager.PREF_PACKAGE_LIST, emptySet())
                ?.contains(lpparam.packageName) == true,
            managedPackages = readManagedPackages(xsp),
            interventionRulesJson = xsp.getString(PrefManager.PREF_INTERVENTION_RULES, "{}") ?: "{}",
        )
    }

    internal fun evaluate(
        packageName: String,
        isSystemApp: Boolean,
        scopeMode: Boolean,
        handleSystem: Boolean,
        packageListDisabled: Boolean,
        managedPackages: Set<String>?,
        interventionRulesJson: String,
    ): ScopeDecision {
        if (packageName in IGNORED_PACKAGES) {
            return noInstall()
        }

        if (!passesSystemFilter(scopeMode, isSystemApp, handleSystem)) {
            return noInstall()
        }

        if (managedPackages == null) {
            return evaluateLegacy(packageListDisabled)
        }

        return evaluateManaged(
            packageName = packageName,
            managedPackages = managedPackages,
            interventionRulesJson = interventionRulesJson,
        )
    }

    private fun evaluateLegacy(packageListDisabled: Boolean): ScopeDecision {
        if (packageListDisabled) {
            return observeOnly()
        }
        return intercept(showNotify = true, crashLogEnabled = true)
    }

    private fun evaluateManaged(
        packageName: String,
        managedPackages: Set<String>,
        interventionRulesJson: String,
    ): ScopeDecision {
        val profile = if (packageName in managedPackages) {
            InterventionRulesCodec.decode(interventionRulesJson)[packageName]
                ?: AppInterventionProfile.EMPTY
        } else {
            AppInterventionProfile.EMPTY
        }

        val enabledRules = profile.rules.filter { it.enabled }
        if (enabledRules.isEmpty()) {
            return observeOnly(crashLogEnabled = true)
        }

        return intercept(
            showNotify = resolveShowNotify(enabledRules, globalDefaultShowNotify = true),
            crashLogEnabled = resolveCrashLogEnabled(enabledRules, defaultEnabled = true),
        )
    }

    private fun passesSystemFilter(
        scopeMode: Boolean,
        isSystemApp: Boolean,
        handleSystem: Boolean,
    ): Boolean = !(scopeMode && isSystemApp && !handleSystem)

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

    private fun resolveShowNotify(enabledRules: List<InterventionRule>, globalDefaultShowNotify: Boolean): Boolean {
        if (enabledRules.isEmpty()) return false
        return enabledRules.any { rule ->
            rule.showNotify ?: globalDefaultShowNotify
        }
    }

    private fun resolveCrashLogEnabled(enabledRules: List<InterventionRule>, defaultEnabled: Boolean): Boolean {
        if (enabledRules.isEmpty()) return defaultEnabled
        return enabledRules.any { rule ->
            rule.crashLogEnabled ?: defaultEnabled
        }
    }

    private fun readManagedPackages(xsp: XSharedPreferences): Set<String>? {
        if (!xsp.contains(PrefManager.PREF_MANAGED_PACKAGES)) {
            return null
        }
        return xsp.getStringSet(PrefManager.PREF_MANAGED_PACKAGES, emptySet()) ?: emptySet()
    }
}
