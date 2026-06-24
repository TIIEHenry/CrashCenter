package nota.android.crash.log

import de.robv.android.xposed.XSharedPreferences
import nota.android.crash.xp.PrefManager

/** Retention limits shared between hook (XSharedPreferences) and module UI. */
object CrashLogRetention {

    data class Limits(
        val maxEntries: Int,
        val maxBytes: Long,
    )

    fun fromXSharedPreferences(): Limits {
        return try {
            val prefs = XSharedPreferences(PrefManager.PACKAGE_NAME, PrefManager.PREF_NAME)
            prefs.reload()
            Limits(
                maxEntries = prefs.getInt(
                    PrefManager.PREF_CRASH_LOG_MAX_ENTRIES,
                    CrashLogJsonlStore.DEFAULT_MAX_ENTRIES,
                ).coerceAtLeast(1),
                maxBytes = prefs.getLong(
                    PrefManager.PREF_CRASH_LOG_MAX_BYTES,
                    CrashLogJsonlStore.DEFAULT_MAX_BYTES,
                ).coerceAtLeast(1024L),
            )
        } catch (_: Throwable) {
            Limits(
                maxEntries = CrashLogJsonlStore.DEFAULT_MAX_ENTRIES,
                maxBytes = CrashLogJsonlStore.DEFAULT_MAX_BYTES,
            )
        }
    }
}
