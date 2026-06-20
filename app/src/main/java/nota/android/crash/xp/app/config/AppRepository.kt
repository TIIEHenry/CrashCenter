package nota.android.crash.xp.app.config

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import nota.android.crash.xp.PrefManager.ITSELF
import nota.android.crash.xp.PrefManager.PREF_HANDLE_SYSTEM
import nota.android.crash.xp.PrefManager.PREF_INTERVENTION_RULES
import nota.android.crash.xp.PrefManager.PREF_MANAGED_PACKAGES
import nota.android.crash.xp.PrefManager.PREF_NAME
import nota.android.crash.xp.PrefManager.PREF_PACKAGE_LIST
import nota.android.crash.xp.PrefManager.PREF_SCOPE_MODE
import nota.android.crash.xp.PrefManager.PREF_SHOW_SYSTEM_UI
import androidx.core.content.edit
import nota.android.crash.xp.app.PackageVisibilityHelper

class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ─── Mode detection ───

    fun isLegacyMode(): Boolean = !prefs.contains(PREF_MANAGED_PACKAGES)

    // ─── Shared preferences (scope / system UI) ───

    fun readScopeMode(): Boolean = prefs.getBoolean(PREF_SCOPE_MODE, false)

    fun readHandleSystem(): Boolean = prefs.getBoolean(PREF_HANDLE_SYSTEM, false)

    fun readShowSystemUi(): Boolean = prefs.getBoolean(PREF_SHOW_SYSTEM_UI, false)

    fun setScopeMode(enabled: Boolean) {
        prefs.edit { putBoolean(PREF_SCOPE_MODE, enabled) }
    }

    fun setHandleSystem(enabled: Boolean) {
        prefs.edit { putBoolean(PREF_HANDLE_SYSTEM, enabled) }
    }

    fun setShowSystemUi(enabled: Boolean) {
        prefs.edit { putBoolean(PREF_SHOW_SYSTEM_UI, enabled) }
    }

    // ─── Package visibility ───

    fun detectPackageVisibility(): PackageVisibilityHelper.Status =
        PackageVisibilityHelper.check(appContext)

    fun detectPackageVisibilityAfterLoad(loadedCount: Int): PackageVisibilityHelper.Status =
        PackageVisibilityHelper.checkAfterLoad(appContext, loadedCount)

    // ─── Legacy: installed apps with hook states ───

    fun loadInstalledApps(): List<AppItem> {
        if (!isLegacyMode()) {
            pruneUninstalled()
            return loadManagedApps().map { managed ->
                AppItem(
                    name = managed.label,
                    icon = managed.icon,
                    hookEnabled = managed.switchChecked,
                    packageName = managed.packageName,
                    isSystemApp = managed.isSystem,
                    updateTime = managed.updateTime,
                    installTime = managed.installTime,
                )
            }
        }

        val prefWhiteList = prefs.getStringSet(PREF_PACKAGE_LIST, null)
        val packageManager = appContext.packageManager
        val installedPackages = PackageVisibilityHelper.getInstalledPackagesCompat(packageManager)
        return installedPackages.map { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@map null
            AppItem(
                name = appInfo.loadLabel(packageManager).toString(),
                icon = appInfo.loadIcon(packageManager),
                hookEnabled = prefWhiteList == null || !prefWhiteList.contains(packageInfo.packageName),
                packageName = packageInfo.packageName,
                isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                updateTime = packageInfo.lastUpdateTime,
                installTime = packageInfo.firstInstallTime,
            )
        }.filterNotNull().filter { app -> app.packageName != ITSELF }
    }

    fun persistHookStates(apps: List<AppItem>) {
        if (!isLegacyMode()) {
            for (app in apps) {
                setInterventionEnabled(app.packageName, app.hookEnabled)
            }
            return
        }

        val disabled = HashSet<String>()
        for (app in apps) {
            if (!app.hookEnabled) {
                disabled.add(app.packageName)
            }
        }
        prefs.edit { putStringSet(PREF_PACKAGE_LIST, disabled) }
    }

    // ─── Managed mode: managed apps ───

    fun readManagedPackageNames(): Set<String>? {
        if (isLegacyMode()) return null
        return HashSet(prefs.getStringSet(PREF_MANAGED_PACKAGES, emptySet()) ?: emptySet())
    }

    fun loadManagedApps(): List<ManagedApp> {
        val managedPackages = readManagedPackageNames() ?: return emptyList()
        if (managedPackages.isEmpty()) return emptyList()

        val profiles = readAllProfiles()
        val packageManager = appContext.packageManager
        val apps = mutableListOf<ManagedApp>()

        for (packageName in managedPackages) {
            if (packageName == ITSELF) continue
            val packageInfo = try {
                packageManager.getPackageInfo(packageName, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                continue
            }
            val appInfo = packageInfo.applicationInfo ?: continue
            val profile = profiles[packageName] ?: AppInterventionProfile.EMPTY
            val enabledCount = profile.enabledRuleCount
            val switchChecked = profile.hasEnabledRule
            apps.add(
                ManagedApp(
                    packageName = packageName,
                    label = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager),
                    isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    interventionStatus = if (switchChecked) {
                        InterventionStatus.ENABLED
                    } else {
                        InterventionStatus.PENDING
                    },
                    switchChecked = switchChecked,
                    enabledRuleCount = enabledCount,
                    summary = if (switchChecked) CATCH_ALL_SUMMARY else null,
                    updateTime = packageInfo.lastUpdateTime,
                    installTime = packageInfo.firstInstallTime,
                ),
            )
        }
        return apps
    }

    fun loadPickableApps(): List<PickableApp> {
        val managedPackages = readManagedPackageNames() ?: emptySet()
        val showSystemUi = readShowSystemUi()
        val packageManager = appContext.packageManager
        val installedPackages = PackageVisibilityHelper.getInstalledPackagesCompat(packageManager)

        val apps = mutableListOf<PickableApp>()
        for (packageInfo in installedPackages) {
            val packageName = packageInfo.packageName
            if (packageName == ITSELF || packageName in managedPackages) continue
            val appInfo = packageInfo.applicationInfo ?: continue
            val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            if (isSystem && !showSystemUi) continue
            apps.add(
                PickableApp(
                    packageName = packageName,
                    label = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager),
                    isSystem = isSystem,
                ),
            )
        }
        return apps
    }

    fun addManagedPackages(packages: Collection<String>) {
        if (packages.isEmpty()) return
        val current = readManagedPackageNames()?.toMutableSet() ?: mutableSetOf()
        var changed = false
        for (packageName in packages) {
            if (packageName == ITSELF) continue
            if (current.add(packageName)) {
                changed = true
            }
        }
        if (!changed) return
        ensureManagedModelActive(current)
    }

    fun removeManagedPackage(packageName: String) {
        val current = readManagedPackageNames()?.toMutableSet() ?: return
        if (!current.remove(packageName)) return
        writeManagedPackages(current)
        val profiles = readAllProfiles().toMutableMap()
        profiles.remove(packageName)
        writeProfiles(profiles)
    }

    fun pruneUninstalled(): Int {
        val managedPackages = readManagedPackageNames()?.toMutableSet() ?: return 0
        if (managedPackages.isEmpty()) return 0

        val packageManager = appContext.packageManager
        val removed = managedPackages.filterTo(mutableSetOf()) { packageName ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
        val prunedCount = managedPackages.size - removed.size
        if (prunedCount == 0) return 0

        writeManagedPackages(removed)
        val profiles = readAllProfiles().toMutableMap()
        for (packageName in managedPackages) {
            if (packageName !in removed) {
                profiles.remove(packageName)
            }
        }
        writeProfiles(profiles)
        return prunedCount
    }

    // ─── Intervention profiles / rules ───

    fun getProfile(packageName: String): AppInterventionProfile =
        readAllProfiles()[packageName] ?: AppInterventionProfile.EMPTY

    fun saveProfile(packageName: String, profile: AppInterventionProfile) {
        val profiles = readAllProfiles().toMutableMap()
        if (profile.rules.isEmpty()) {
            profiles.remove(packageName)
        } else {
            profiles[packageName] = profile.copy(updatedAt = System.currentTimeMillis())
        }
        writeProfiles(profiles)
    }

    fun setInterventionEnabled(packageName: String, enabled: Boolean) {
        val profile = getProfile(packageName)
        val updated = if (enabled) {
            enableIntervention(profile)
        } else {
            disableIntervention(profile)
        }
        saveProfile(packageName, updated)
    }

    // ─── Private helpers ───

    private fun enableIntervention(profile: AppInterventionProfile): AppInterventionProfile {
        if (profile.rules.isEmpty()) {
            return AppInterventionProfile(
                rules = listOf(InterventionRule.defaultCatchAll(enabled = true)),
            )
        }
        val hasEnabled = profile.rules.any { it.enabled }
        if (hasEnabled) return profile
        val updatedRules = profile.rules.map { rule ->
            if (rule.type == InterventionRuleType.CATCH_ALL) {
                rule.copy(enabled = true)
            } else {
                rule
            }
        }
        val enabledRules = if (updatedRules.any { it.enabled }) {
            updatedRules
        } else {
            updatedRules.mapIndexed { index, rule ->
                if (index == 0) rule.copy(enabled = true) else rule
            }
        }
        return profile.copy(rules = enabledRules)
    }

    private fun disableIntervention(profile: AppInterventionProfile): AppInterventionProfile {
        if (profile.rules.isEmpty()) return profile
        return profile.copy(
            rules = profile.rules.map { it.copy(enabled = false) },
        )
    }

    private fun readAllProfiles(): Map<String, AppInterventionProfile> {
        val json = prefs.getString(PREF_INTERVENTION_RULES, "{}") ?: "{}"
        return InterventionRulesCodec.decode(json)
    }

    private fun writeProfiles(profiles: Map<String, AppInterventionProfile>) {
        prefs.edit {
            putString(PREF_INTERVENTION_RULES, InterventionRulesCodec.encode(profiles))
        }
    }

    private fun writeManagedPackages(packages: Set<String>) {
        prefs.edit { putStringSet(PREF_MANAGED_PACKAGES, HashSet(packages)) }
    }

    private fun ensureManagedModelActive(packages: Set<String>) {
        prefs.edit { putStringSet(PREF_MANAGED_PACKAGES, HashSet(packages)) }
    }

    companion object {
        private const val CATCH_ALL_SUMMARY = "CATCH_ALL"

        internal fun enumerateInstalledPackageNames(context: Context): Set<String> {
            val packageManager = context.packageManager
            val installedPackages = PackageVisibilityHelper.getInstalledPackagesCompat(packageManager)
            return installedPackages.map { it.packageName }
                .filter { packageName -> packageName != ITSELF }
                .toSet()
        }

        internal fun passesSystemFilter(isSystemApp: Boolean, handleSystem: Boolean): Boolean =
            !isSystemApp || handleSystem

        internal fun isSystemPackage(context: Context, packageName: String): Boolean {
            return try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}
