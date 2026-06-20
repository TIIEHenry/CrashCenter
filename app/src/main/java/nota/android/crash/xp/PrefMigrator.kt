package nota.android.crash.xp

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Xml
import androidx.core.content.edit
import nota.android.crash.xp.app.config.AppInterventionProfile
import nota.android.crash.xp.app.config.InterventionRule
import nota.android.crash.xp.app.config.InterventionRulesCodec
import nota.android.crash.xp.app.config.AppRepository
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * One-shot migration from legacy package prefs (`tiiehenry.xp.grapcrash` / `grapcrash.xml`).
 * Runs once per install; no ongoing reads of legacy paths after [KEY_MIGRATED] is set.
 */
object PrefMigrator {

    private const val LEGACY_PACKAGE = "tiiehenry.xp.grapcrash"
    private const val LEGACY_PREF_FILE = "grapcrash"
    private const val KEY_MIGRATED = "legacy_prefs_migrated"
    private const val UPGRADE_TIME_GRACE_MS = 60_000L

    private val BOOL_KEYS = listOf(
        PrefManager.PREF_SCOPE_MODE,
        PrefManager.PREF_HANDLE_SYSTEM,
        PrefManager.PREF_SHOW_SYSTEM_UI,
    )

    /**
     * @return whether [KEY_MIGRATED] was already set before this call (prior app session),
     *         and whether legacy grapcrash prefs supplied data on this call.
     */
    fun migrateIfNeeded(context: Context): LegacyPrefState {
        val dest = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        val hadPriorSession = dest.getBoolean(KEY_MIGRATED, false)
        if (hadPriorSession) {
            return LegacyPrefState(hadPriorSession = true, importHadData = false)
        }

        val snapshot = readLegacySnapshot(context)
        val importHadData = snapshot != null && snapshot.hasData()
        if (importHadData) {
            dest.edit {
                snapshot.booleans.forEach { (key, value) -> putBoolean(key, value) }
                snapshot.stringSets.forEach { (key, value) ->
                    putStringSet(key, HashSet(value))
                }
            }
        }

        dest.edit { putBoolean(KEY_MIGRATED, true) }
        return LegacyPrefState(hadPriorSession = false, importHadData = importHadData)
    }

    /**
     * One-shot migration from ADR-002 `package_list` to ADR-015 managed model.
     * Runs once per install after [migrateIfNeeded]; leaves `package_list` read-only.
     */
    fun migrateManagedModelIfNeeded(context: Context, legacyPrefState: LegacyPrefState) {
        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false)) {
            return
        }
        if (prefs.contains(PrefManager.PREF_MANAGED_PACKAGES)) {
            prefs.edit { putBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, true) }
            return
        }

        if (!shouldMigrateFromLegacyManagedModel(context, prefs, legacyPrefState)) {
            activateEmptyManagedModel(prefs)
            return
        }

        migrateLegacyManagedModel(context, prefs)
    }

    /**
     * Fresh installs activate managed model with an empty curator list (ADR-015 §新用户).
     * Legacy upgrades derive managed_packages from package_list + installed apps.
     */
    private fun shouldMigrateFromLegacyManagedModel(
        context: Context,
        prefs: SharedPreferences,
        legacyPrefState: LegacyPrefState,
    ): Boolean {
        if (legacyPrefState.importHadData) return true
        if (prefs.contains(PrefManager.PREF_PACKAGE_LIST)) return true
        if (prefs.getBoolean(PrefManager.PREF_SCOPE_MODE, false)) return true
        if (prefs.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false)) return true
        if (prefs.getBoolean(PrefManager.PREF_SHOW_SYSTEM_UI, false)) return true
        // Prior session prefs (APK update without pm clear) + legacy default-all-hook upgrade path.
        // pm clear / fresh install: hadPriorSession=false → empty managed list (ADR-015 §新用户).
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

    private fun activateEmptyManagedModel(prefs: SharedPreferences) {
        prefs.edit {
            putStringSet(PrefManager.PREF_MANAGED_PACKAGES, HashSet())
            putString(PrefManager.PREF_INTERVENTION_RULES, "{}")
            putBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, true)
        }
    }

    private fun migrateLegacyManagedModel(context: Context, prefs: SharedPreferences) {
        val scopeMode = prefs.getBoolean(PrefManager.PREF_SCOPE_MODE, false)
        val handleSystem = prefs.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false)
        val disabled = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, emptySet()) ?: emptySet()
        val installed = AppRepository.enumerateInstalledPackageNames(context)

        val managed = installed.filter { packageName ->
            packageName !in disabled
        }.filter { packageName ->
            if (!scopeMode) {
                true
            } else {
                val isSystem = AppRepository.isSystemPackage(context, packageName)
                AppRepository.passesSystemFilter(isSystem, handleSystem)
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

    data class LegacyPrefState(
        val hadPriorSession: Boolean,
        val importHadData: Boolean,
    )

    private data class Snapshot(
        val booleans: Map<String, Boolean> = emptyMap(),
        val stringSets: Map<String, Set<String>> = emptyMap(),
    ) {
        fun hasData(): Boolean = booleans.isNotEmpty() || stringSets.isNotEmpty()
    }

    private fun readLegacySnapshot(context: Context): Snapshot? {
        readViaPackageContext(context)?.takeIf { it.hasData() }?.let { return it }
        readViaRoot(context)?.takeIf { it.hasData() }?.let { return it }
        return null
    }

    private fun readViaPackageContext(context: Context): Snapshot? {
        return try {
            val legacyContext = context.createPackageContext(
                LEGACY_PACKAGE,
                Context.CONTEXT_IGNORE_SECURITY,
            )
            val legacy = legacyContext.getSharedPreferences(LEGACY_PREF_FILE, Context.MODE_PRIVATE)
            snapshotFromPrefs(legacy)
        } catch (_: Exception) {
            null
        }
    }

    private fun snapshotFromPrefs(prefs: SharedPreferences): Snapshot {
        val booleans = mutableMapOf<String, Boolean>()
        val stringSets = mutableMapOf<String, Set<String>>()

        for (key in BOOL_KEYS) {
            if (prefs.contains(key)) {
                booleans[key] = prefs.getBoolean(key, false)
            }
        }
        if (prefs.contains(PrefManager.PREF_PACKAGE_LIST)) {
            val set = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, emptySet()) ?: emptySet()
            stringSets[PrefManager.PREF_PACKAGE_LIST] = set
        }

        return Snapshot(booleans, stringSets)
    }

    private fun readViaRoot(context: Context): Snapshot? {
        val path = "${context.filesDir.parentFile}/$LEGACY_PACKAGE/shared_prefs/$LEGACY_PREF_FILE.xml"
        val xml = execSuCat(path) ?: return null
        return parsePrefsXml(xml)
    }

    private fun execSuCat(path: String): String? {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $path"))
            val text = proc.inputStream.bufferedReader().readText()
            if (proc.waitFor() == 0 && text.isNotBlank()) text else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePrefsXml(xml: String): Snapshot? {
        return try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            val booleans = mutableMapOf<String, Boolean>()
            val stringSets = mutableMapOf<String, Set<String>>()

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event != XmlPullParser.START_TAG) {
                    event = parser.next()
                    continue
                }

                when (parser.name) {
                    "boolean" -> {
                        val name = parser.getAttributeValue(null, "name")
                        val value = parser.getAttributeValue(null, "value")
                        if (name != null && value != null && BOOL_KEYS.contains(name)) {
                            booleans[name] = value == "true"
                        }
                    }
                    "set" -> {
                        val name = parser.getAttributeValue(null, "name")
                        if (name == PrefManager.PREF_PACKAGE_LIST) {
                            stringSets[name] = readStringSetChildren(parser)
                        }
                    }
                }
                event = parser.next()
            }

            Snapshot(booleans, stringSets)
        } catch (_: Exception) {
            null
        }
    }

    private fun readStringSetChildren(parser: XmlPullParser): Set<String> {
        val values = HashSet<String>()
        var event = parser.next()
        while (event != XmlPullParser.END_TAG || parser.name != "set") {
            if (event == XmlPullParser.START_TAG && parser.name == "string") {
                values.add(parser.nextText())
            }
            event = parser.next()
        }
        return values
    }
}
