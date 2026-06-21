package nota.android.crash.xp.migration

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Imports boolean/string-set values from a legacy snapshot into current prefs.
 */
object LegacyPrefImporter {

    fun import(dest: SharedPreferences, snapshot: LegacyPrefSnapshotReader.Snapshot): Boolean {
        val importHadData = snapshot.hasData()
        if (!importHadData) return false

        dest.edit {
            snapshot.booleans.forEach { (key, value) -> putBoolean(key, value) }
            snapshot.stringSets.forEach { (key, value) ->
                putStringSet(key, HashSet(value))
            }
        }
        return true
    }
}
