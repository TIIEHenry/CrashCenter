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
            return ScopeDecision(shouldHook = false, showNotify = false)
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
            return ScopeDecision(shouldHook = false, showNotify = false)
        }

        if (managedPackages == null) {
            return evaluateLegacy(
                packageName = packageName,
                isSystemApp = isSystemApp,
                scopeMode = scopeMode,
                handleSystem = handleSystem,
                disabled = packageListDisabled,
            )
        }

        return evaluateManaged(
            packageName = packageName,
            isSystemApp = isSystemApp,
            scopeMode = scopeMode,
            handleSystem = handleSystem,
            managedPackages = managedPackages,
            interventionRulesJson = interventionRulesJson,
        )
    }

    private fun evaluateLegacy(
        packageName: String,
        isSystemApp: Boolean,
        scopeMode: Boolean,
        handleSystem: Boolean,
        disabled: Boolean,
    ): ScopeDecision {
        if (scopeMode) {
            val shouldHook = (!isSystemApp || handleSystem) && !disabled
            return ScopeDecision(shouldHook = shouldHook, showNotify = true)
        }
        return ScopeDecision(shouldHook = true, showNotify = !disabled)
    }

    private fun evaluateManaged(
        packageName: String,
        isSystemApp: Boolean,
        scopeMode: Boolean,
        handleSystem: Boolean,
        managedPackages: Set<String>,
        interventionRulesJson: String,
    ): ScopeDecision {
        if (packageName !in managedPackages) {
            return ScopeDecision(shouldHook = false, showNotify = false)
        }

        val profile = InterventionRulesCodec.decode(interventionRulesJson)[packageName]
            ?: AppInterventionProfile.EMPTY
        val enabledRules = profile.rules.filter { it.enabled }
        if (enabledRules.isEmpty()) {
            return ScopeDecision(shouldHook = false, showNotify = false)
        }

        if (scopeMode && isSystemApp && !handleSystem) {
            return ScopeDecision(shouldHook = false, showNotify = false)
        }

        // Managed hooked packages inherit showNotify=true when rules leave it null
        // (same as legacy scope_mode on/off for non-disabled packages).
        val showNotify = resolveShowNotify(enabledRules, globalDefaultShowNotify = true)
        val crashLogEnabled = resolveCrashLogEnabled(enabledRules, defaultEnabled = true)
        return ScopeDecision(
            shouldHook = true,
            showNotify = showNotify,
            crashLogEnabled = crashLogEnabled,
        )
    }

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
