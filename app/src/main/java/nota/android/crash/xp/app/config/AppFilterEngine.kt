package nota.android.crash.xp.app.config

import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.CrashFilter
import java.util.Locale

object AppFilterEngine {

    fun sort(list: List<AppItem>, mode: SortMode): List<AppItem> {
        return when (mode) {
            SortMode.NAME_ASC -> list.sortedBy { it.label }
            SortMode.NAME_DESC -> list.sortedByDescending { it.label }
            SortMode.INSTALL_TIME_ASC -> list.sortedBy { it.installTime }
            SortMode.INSTALL_TIME_DESC -> list.sortedByDescending { it.installTime }
            SortMode.UPDATE_TIME_ASC -> list.sortedBy { it.updateTime }
            SortMode.UPDATE_TIME_DESC -> list.sortedByDescending { it.updateTime }
        }
    }

    fun matchesCrashEvent(event: CrashEvent, filter: CrashFilter): Boolean {
        filter.packageName?.let { if (event.packageName != it) return false }
        filter.exceptionClass?.let { if (event.exceptionClass != it) return false }
        filter.source?.let { if (event.source != it) return false }
        filter.sinceMs?.let { if (event.timestampMs < it) return false }
        filter.untilMs?.let { if (event.timestampMs > it) return false }
        filter.query?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            val q = query.lowercase(Locale.ROOT)
            val haystack = listOfNotNull(
                event.appLabel,
                event.packageName,
                event.exceptionClass,
                event.message,
            ).joinToString(" ").lowercase(Locale.ROOT)
            if (!haystack.contains(q)) return false
        }
        return true
    }
}
