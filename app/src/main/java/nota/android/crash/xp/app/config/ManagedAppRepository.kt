package nota.android.crash.xp.app.config

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import nota.android.crash.xp.PrefManager.PREF_INTERVENTION_RULES
import nota.android.crash.xp.PrefManager.PREF_MANAGED_PACKAGES
import nota.android.crash.xp.PrefManager.PREF_NAME
import nota.android.crash.xp.app.PackageVisibilityHelper

/**
 * Handles managed packages, profiles, and intervention rules.
 */
class ManagedAppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val packageManager = appContext.packageManager

    // ─── Mode detection ───

    fun isLegacyMode(): Boolean = !prefs.contains(PREF_MANAGED_PACKAGES)

    // ─── Managed packages ───

    fun readManagedPackageNames(): Set<String>? {
        if (isLegacyMode()) return null
        return buildSet {
            addAll(prefs.getStringSet(PREF_MANAGED_PACKAGES, emptySet()) ?: emptySet())
        }
    }

    fun loadManagedApps(): List<ManagedApp> {
        val managedPackages = readManagedPackageNames() ?: return emptyList()
        if (managedPackages.isEmpty()) return emptyList()

        val profiles = readAllProfiles()

        return buildList {
            for (packageName in managedPackages) {
                if (PackageInfoLoader.isItself(packageName)) continue
                val (packageInfo, appInfo) = PackageInfoLoader.loadAppInfo(packageManager, packageName)
                    ?: continue
                val profile = profiles[packageName] ?: AppInterventionProfile.EMPTY
                val enabledCount = profile.enabledRuleCount
                val switchChecked = profile.hasEnabledRule
                add(
                    ManagedApp(
                        packageName = packageName,
                        label = PackageInfoLoader.loadLabel(packageManager, appInfo),
                        appInfo = appInfo,
                        isSystem = PackageInfoLoader.isSystemApp(appInfo),
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
        }
    }

    fun loadPickableApps(): List<PickableApp> {
        val managedPackages = readManagedPackageNames() ?: emptySet()
        val showSystemUi = prefs.getBoolean("show_system_ui", false)
        val installedPackages = PackageVisibilityHelper.getInstalledPackagesCompat(packageManager)

        return buildList {
            for (packageInfo in installedPackages) {
                val packageName = packageInfo.packageName
                if (PackageInfoLoader.isItself(packageName) || packageName in managedPackages) continue
                val appInfo = packageInfo.applicationInfo ?: continue
                val isSystem = PackageInfoLoader.isSystemApp(appInfo)
                if (isSystem && !showSystemUi) continue
                add(
                    PickableApp(
                        packageName = packageName,
                        label = PackageInfoLoader.loadLabel(packageManager, appInfo),
                        appInfo = appInfo,
                        isSystem = isSystem,
                    ),
                )
            }
        }
    }

    fun addManagedPackages(packages: Collection<String>) {
        if (packages.isEmpty()) return
        val current = readManagedPackageNames() ?: emptySet()
        val newPackages = packages.filter { !PackageInfoLoader.isItself(it) && it !in current }
        if (newPackages.isEmpty()) return
        val merged = current + newPackages
        writeManagedPackages(merged)
    }

    fun removeManagedPackage(packageName: String) {
        val current = readManagedPackageNames() ?: return
        if (packageName !in current) return
        val updated = current - packageName
        writeManagedPackages(updated)
        val profiles = readAllProfiles()
        writeProfiles(profiles - packageName)
    }

    fun pruneUninstalled(): Int {
        val managedPackages = readManagedPackageNames() ?: return 0
        if (managedPackages.isEmpty()) return 0

        val installed = buildSet {
            for (packageName in managedPackages) {
                if (PackageInfoLoader.loadAppInfo(packageManager, packageName) != null) {
                    add(packageName)
                }
            }
        }
        val prunedCount = managedPackages.size - installed.size
        if (prunedCount == 0) return 0

        writeManagedPackages(installed)
        val profiles = readAllProfiles()
        writeProfiles(profiles.filterKeys { it in installed })
        return prunedCount
    }

    // ─── Intervention profiles / rules ───

    fun getProfile(packageName: String): AppInterventionProfile =
        readAllProfiles()[packageName] ?: AppInterventionProfile.EMPTY

    fun saveProfile(packageName: String, profile: AppInterventionProfile) {
        val profiles = readAllProfiles()
        val updated = if (profile.rules.isEmpty()) {
            profiles - packageName
        } else {
            profiles + (packageName to profile.copy(updatedAt = System.currentTimeMillis()))
        }
        writeProfiles(updated)
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
        prefs.edit { putStringSet(PREF_MANAGED_PACKAGES, packages.toSet()) }
    }

    companion object {
        private const val CATCH_ALL_SUMMARY = "CATCH_ALL"
    }
}
