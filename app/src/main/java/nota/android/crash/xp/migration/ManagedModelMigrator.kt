package nota.android.crash.xp.migration

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.PrefMigrator
import nota.android.crash.xp.app.config.AppInterventionProfile
import nota.android.crash.xp.app.config.InterventionRule
import nota.android.crash.xp.app.config.InterventionRulesCodec
import nota.android.crash.xp.app.config.PackageInfoLoader

/**
 * Handles ADR-002 `package_list` → ADR-015 managed model migration.
 */
object ManagedModelMigrator {

    private const val UPGRADE_TIME_GRACE_MS = 60_000L

    fun migrateIfNeeded(context: Context, prefs: SharedPreferences, legacyPrefState: PrefMigrator.LegacyPrefState) {
        if (prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false)) {
            return
        }
        if (prefs.contains(PrefManager.PREF_MANAGED_PACKAGES)) {
            prefs.edit { putBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, true) }
            return
        }

        if (!shouldMigrateFromLegacy(context, prefs, legacyPrefState)) {
            activateEmpty(prefs)
            return
        }

        migrateFromLegacy(context, prefs)
    }

    private fun shouldMigrateFromLegacy(
        context: Context,
        prefs: SharedPreferences,
        legacyPrefState: PrefMigrator.LegacyPrefState,
    ): Boolean {
        if (legacyPrefState.importHadData) return true
        if (prefs.contains(PrefManager.PREF_PACKAGE_LIST)) return true
        if (prefs.getBoolean(PrefManager.PREF_SCOPE_MODE, false)) return true
        if (prefs.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false)) return true
        if (prefs.getBoolean(PrefManager.PREF_SHOW_SYSTEM_UI, false)) return true
        return legacyPrefState.hadPriorSession && isAppUpgrade(context)
    }

    private fun isAppUpgrade(context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.lastUpdateTime - packageInfo.firstInstallTime > UPGRADE_TIME_GRACE_MS
        } catch (_: Exception) {
            false
        }
    }

    private fun activateEmpty(prefs: SharedPreferences) {
        prefs.edit {
            putStringSet(PrefManager.PREF_MANAGED_PACKAGES, HashSet())
            putString(PrefManager.PREF_INTERVENTION_RULES, "{}")
            putBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, true)
        }
    }

    private fun migrateFromLegacy(context: Context, prefs: SharedPreferences) {
        val scopeMode = prefs.getBoolean(PrefManager.PREF_SCOPE_MODE, false)
        val handleSystem = prefs.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false)
        val disabled = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, emptySet()) ?: emptySet()
        val installed = PackageInfoLoader.enumerateInstalledPackageNames(context)

        val managed = installed.filter { packageName ->
            packageName !in disabled
        }.filter { packageName ->
            if (!scopeMode) {
                true
            } else {
                val isSystem = PackageInfoLoader.isSystemPackage(context, packageName)
                PackageInfoLoader.passesSystemFilter(isSystem, handleSystem)
            }
        }.toSet()

        val rules = managed.associateWith {
            AppInterventionProfile(
                rules = listOf(InterventionRule.defaultCatchAll(enabled = true)),
            )
        }

        prefs.edit {
            putStringSet(PrefManager.PREF_MANAGED_PACKAGES, HashSet(managed))
            putString(
                PrefManager.PREF_INTERVENTION_RULES,
                InterventionRulesCodec.encode(rules),
            )
            putBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, true)
        }
    }
}
