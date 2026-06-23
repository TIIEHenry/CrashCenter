package nota.android.crash.xp

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import nota.android.crash.root.RootAccessClient
import nota.android.crash.xp.migration.LegacyPrefImporter
import nota.android.crash.xp.migration.LegacyPrefSnapshotReader
import nota.android.crash.xp.migration.ManagedModelMigrator

/**
 * One-shot migration from legacy package prefs (`tiiehenry.xp.grapcrash` / `grapcrash.xml`).
 * Runs once per install; no ongoing reads of legacy paths after [KEY_MIGRATED] is set.
 *
 * Coordinates three focused steps:
 * 1. [LegacyPrefSnapshotReader] — reads legacy prefs snapshot
 * 2. [LegacyPrefImporter] — imports boolean/string-set values into current prefs
 * 3. [ManagedModelMigrator] — migrates ADR-002 `package_list` → ADR-015 managed model
 */
object PrefMigrator {

    private const val KEY_MIGRATED = "legacy_prefs_migrated"

    /**
     * @return whether [KEY_MIGRATED] was already set before this call (prior app session),
     *         and whether legacy grapcrash prefs supplied data on this call.
     */
    fun migrateIfNeeded(context: Context, prefs: SharedPreferences, rootAccessClient: RootAccessClient): LegacyPrefState {
        val hadPriorSession = prefs.getBoolean(KEY_MIGRATED, false)
        if (hadPriorSession) {
            return LegacyPrefState(hadPriorSession = true, importHadData = false)
        }

        val snapshot = LegacyPrefSnapshotReader(rootAccessClient).read(context)
        val importHadData = snapshot != null && LegacyPrefImporter.import(prefs, snapshot)

        prefs.edit { putBoolean(KEY_MIGRATED, true) }
        return LegacyPrefState(hadPriorSession = false, importHadData = importHadData)
    }

    /**
     * One-shot migration from ADR-002 `package_list` to ADR-015 managed model.
     * Runs once per install after [migrateIfNeeded]; leaves `package_list` read-only.
     */
    fun migrateManagedModelIfNeeded(context: Context, prefs: SharedPreferences, legacyPrefState: LegacyPrefState) {
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyPrefState)
    }

    /** ADR-023: behavioral migration marker; no prefs rewrite required. */
    fun migrateObserveInterceptSplitIfNeeded(prefs: SharedPreferences) {
        if (prefs.getBoolean(PrefManager.PREF_OBSERVE_INTERCEPT_SPLIT_MIGRATED, false)) {
            return
        }
        prefs.edit { putBoolean(PrefManager.PREF_OBSERVE_INTERCEPT_SPLIT_MIGRATED, true) }
    }

    data class LegacyPrefState(
        val hadPriorSession: Boolean,
        val importHadData: Boolean,
    )
}
