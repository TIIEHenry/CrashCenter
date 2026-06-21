package nota.android.crash.xp.app.config

import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.CrashFilter
import java.util.Locale

object AppFilterEngine {

    fun filterLegacyApps(
        apps: List<AppItem>,
        query: String,
        hookFilter: HookFilter,
        showSystemUi: Boolean,
    ): List<AppItem> {
        val filtered = apps.filter { app ->
            val systemMatch = showSystemUi && app.isSystem ||
                !showSystemUi && !app.isSystem
            if (!systemMatch) return@filter false

            val hookMatch = when (hookFilter) {
                HookFilter.ON -> app.hookEnabled
                HookFilter.OFF -> !app.hookEnabled
                HookFilter.ALL -> true
            }
            hookMatch
        }
        return filterByQuery(filtered, query, { it.label }, { it.packageName })
    }

    fun filterManagedApps(
        apps: List<ManagedApp>,
        query: String,
        managedFilter: ManagedFilter,
    ): List<ManagedApp> {
        val filtered = apps.filter { app ->
            when (managedFilter) {
                ManagedFilter.ENABLED -> app.interventionStatus == InterventionStatus.ENABLED
                ManagedFilter.PENDING -> app.interventionStatus == InterventionStatus.PENDING
                ManagedFilter.ALL -> true
            }
        }
        return filterByQuery(filtered, query, { it.label }, { it.packageName })
    }

    fun matchesCrashEvent(event: CrashEvent, filter: CrashFilter): Boolean {
        filter.packageName?.let { if (event.packageName != it) return false }
        filter.source?.let { if (event.source != it) return false }
        filter.sinceMs?.let { if (event.timestampMs < it) return false }
        filter.untilMs?.let { if (event.timestampMs > it) return false }
        filter.query?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            val q = query.lowercase(Locale.getDefault())
            val haystack = listOfNotNull(
                event.appLabel,
                event.packageName,
                event.exceptionClass,
                event.message,
            ).joinToString(" ").lowercase(Locale.getDefault())
            if (!haystack.contains(q)) return false
        }
        return true
    }

    fun <T> filterByQuery(
        items: List<T>,
        query: String,
        labelExtractor: (T) -> String,
        packageNameExtractor: (T) -> String,
    ): List<T> {
        val queryLower = query.lowercase(Locale.getDefault())
        if (queryLower.isEmpty()) return items
        return items.filter { item ->
            labelExtractor(item).lowercase(Locale.getDefault()).contains(queryLower) ||
                packageNameExtractor(item).lowercase(Locale.getDefault()).contains(queryLower)
        }
    }

    fun <T : AppListItem> sort(
        list: MutableList<T>,
        mode: SortMode,
    ) {
        when (mode) {
            SortMode.NAME_ASC -> list.sortWith(compareBy { it.label })
            SortMode.NAME_DESC -> list.sortWith(compareByDescending { it.label })
            SortMode.INSTALL_TIME_ASC -> list.sortWith(compareBy { it.installTime })
            SortMode.INSTALL_TIME_DESC -> list.sortWith(compareByDescending { it.installTime })
            SortMode.UPDATE_TIME_ASC -> list.sortWith(compareBy { it.updateTime })
            SortMode.UPDATE_TIME_DESC -> list.sortWith(compareByDescending { it.updateTime })
        }
    }
}
