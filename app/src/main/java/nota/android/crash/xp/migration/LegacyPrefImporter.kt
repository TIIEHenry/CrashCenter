package nota.android.crash.xp.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import nota.android.crash.xp.PrefManager

/**
 * Imports boolean/string-set values from a legacy snapshot into current prefs.
 */
object LegacyPrefImporter {

    fun import(context: Context, snapshot: LegacyPrefSnapshotReader.Snapshot): Boolean {
        val importHadData = snapshot.hasData()
        if (!importHadData) return false

        val dest = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        dest.edit {
            snapshot.booleans.forEach { (key, value) -> putBoolean(key, value) }
            snapshot.stringSets.forEach { (key, value) ->
                putStringSet(key, HashSet(value))
            }
        }
        return true
    }
}
