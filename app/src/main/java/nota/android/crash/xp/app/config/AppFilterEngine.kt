package nota.android.crash.xp.app.config

import java.util.Locale

object AppFilterEngine {

    fun filterLegacyApps(
        apps: List<AppItem>,
        query: String,
        hookFilter: HookFilter,
        showSystemUi: Boolean,
    ): List<AppItem> {
        val queryLower = query.lowercase(Locale.getDefault())
        return apps.filter { app ->
            val systemMatch = showSystemUi && app.isSystemApp ||
                !showSystemUi && !app.isSystemApp
            if (!systemMatch) return@filter false

            val hookMatch = when (hookFilter) {
                HookFilter.ON -> app.hookEnabled
                HookFilter.OFF -> !app.hookEnabled
                HookFilter.ALL -> true
            }
            if (!hookMatch) return@filter false

            if (queryLower.isEmpty()) return@filter true
            app.name.lowercase(Locale.getDefault()).contains(queryLower) ||
                app.packageName.lowercase(Locale.getDefault()).contains(queryLower)
        }
    }

    fun filterManagedApps(
        apps: List<ManagedApp>,
        query: String,
        managedFilter: ManagedFilter,
    ): List<ManagedApp> {
        val queryLower = query.lowercase(Locale.getDefault())
        return apps.filter { app ->
            val filterMatch = when (managedFilter) {
                ManagedFilter.ENABLED -> app.interventionStatus == InterventionStatus.ENABLED
                ManagedFilter.PENDING -> app.interventionStatus == InterventionStatus.PENDING
                ManagedFilter.ALL -> true
            }
            if (!filterMatch) return@filter false

            if (queryLower.isEmpty()) return@filter true
            app.label.lowercase(Locale.getDefault()).contains(queryLower) ||
                app.packageName.lowercase(Locale.getDefault()).contains(queryLower)
        }
    }

    fun <T> sort(
        list: MutableList<T>,
        mode: SortMode,
        nameExtractor: (T) -> String,
        installTimeExtractor: (T) -> Long,
        updateTimeExtractor: (T) -> Long,
    ) {
        when (mode) {
            SortMode.NAME_ASC -> list.sortWith(compareBy { nameExtractor(it) })
            SortMode.NAME_DESC -> list.sortWith(compareByDescending { nameExtractor(it) })
            SortMode.INSTALL_TIME_ASC -> list.sortWith(compareBy { installTimeExtractor(it) })
            SortMode.INSTALL_TIME_DESC -> list.sortWith(compareByDescending { installTimeExtractor(it) })
            SortMode.UPDATE_TIME_ASC -> list.sortWith(compareBy { updateTimeExtractor(it) })
            SortMode.UPDATE_TIME_DESC -> list.sortWith(compareByDescending { updateTimeExtractor(it) })
        }
    }
}
