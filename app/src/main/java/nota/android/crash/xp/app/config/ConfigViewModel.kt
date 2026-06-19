package nota.android.crash.xp.app.config

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.Collections
import java.util.Locale

class ConfigViewModel(
    private val legacyRepository: AppListRepository,
    private val managedRepository: ManagedAppRepository,
) : ViewModel() {

    private val _uiState = MutableLiveData(ConfigUiState())
    val uiState: LiveData<ConfigUiState> = _uiState

    private var appsLoadGeneration = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        val legacyMode = managedRepository.isLegacyMode()
        emitState {
            copy(
                scopeMode = managedRepository.readScopeMode(),
                handleSystem = managedRepository.readHandleSystem(),
                showSystemUi = managedRepository.readShowSystemUi(),
                packageVisibility = managedRepository.detectPackageVisibility(),
                isLegacyMode = legacyMode,
            )
        }
    }

    fun loadApps(forceReload: Boolean = false) {
        val current = _uiState.value ?: ConfigUiState()
        if (!forceReload) {
            if (current.isLegacyMode && current.allApps.isNotEmpty()) return
            if (!current.isLegacyMode && current.managedApps.isNotEmpty()) return
        }

        val loadGeneration = ++appsLoadGeneration
        emitState { copy(isLoading = true) }

        Thread {
            try {
                if (current.isLegacyMode) {
                    val loadedApps = legacyRepository.loadInstalledApps()
                    val visibility = legacyRepository.detectPackageVisibilityAfterLoad(loadedApps.size)
                    mainHandler.post {
                        if (loadGeneration != appsLoadGeneration) return@post
                        emitState {
                            copy(
                                isLoading = false,
                                allApps = loadedApps,
                                packageVisibility = visibility,
                            )
                        }
                        applyLegacyFiltersAndSort(preserveSort = false)
                    }
                } else {
                    managedRepository.pruneUninstalled()
                    val managedApps = managedRepository.loadManagedApps()
                    val visibility = managedRepository.detectPackageVisibility()
                    mainHandler.post {
                        if (loadGeneration != appsLoadGeneration) return@post
                        emitState {
                            copy(
                                isLoading = false,
                                managedApps = managedApps,
                                packageVisibility = visibility,
                            )
                        }
                        applyManagedFiltersAndSort(preserveSort = false)
                    }
                }
            } catch (_: Exception) {
                mainHandler.post {
                    if (loadGeneration != appsLoadGeneration) return@post
                    emitState { copy(isLoading = false) }
                }
            }
        }.start()
    }

    fun setQuery(query: String) {
        emitState { copy(query = query) }
        applyCurrentFilters(preserveSort = true)
    }

    fun setHookFilter(filter: HookFilter) {
        emitState { copy(hookFilter = filter) }
        applyCurrentFilters(preserveSort = true)
    }

    fun setManagedFilter(filter: ManagedFilter) {
        emitState { copy(managedFilter = filter) }
        applyCurrentFilters(preserveSort = true)
    }

    fun setScopeMode(enabled: Boolean) {
        managedRepository.setScopeMode(enabled)
        emitState { copy(scopeMode = enabled) }
    }

    fun setHandleSystem(enabled: Boolean) {
        managedRepository.setHandleSystem(enabled)
        emitState { copy(handleSystem = enabled) }
    }

    fun setShowSystemUi(enabled: Boolean) {
        managedRepository.setShowSystemUi(enabled)
        emitState { copy(showSystemUi = enabled) }
        applyCurrentFilters(preserveSort = true)
    }

    fun toggleApp(packageName: String) {
        val current = _uiState.value ?: return
        if (current.isLegacyMode) {
            val updated = current.allApps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(hookEnabled = !app.hookEnabled)
                } else {
                    app
                }
            }
            emitState { copy(allApps = updated) }
            legacyRepository.persistHookStates(updated)
            applyLegacyFiltersAndSort(preserveSort = true)
        }
    }

    fun setManagedSwitch(packageName: String, enabled: Boolean) {
        val current = _uiState.value ?: return
        if (current.isLegacyMode) return

        managedRepository.setInterventionEnabled(packageName, enabled)
        val updated = current.managedApps.map { app ->
            if (app.packageName == packageName) {
                reloadManagedApp(app.packageName) ?: app.copy(
                    switchChecked = enabled,
                    interventionStatus = if (enabled) {
                        InterventionStatus.ENABLED
                    } else {
                        InterventionStatus.PENDING
                    },
                    enabledRuleCount = if (enabled) 1 else 0,
                )
            } else {
                app
            }
        }
        emitState { copy(managedApps = updated) }
        applyManagedFiltersAndSort(preserveSort = true)
    }

    fun addManagedPackages(packages: Collection<String>) {
        if (packages.isEmpty()) return
        managedRepository.addManagedPackages(packages)
        loadApps(forceReload = true)
    }

    fun selectAll(enabled: Boolean) {
        val current = _uiState.value ?: return
        if (!current.isLegacyMode) return
        val updated = current.allApps.map { it.copy(hookEnabled = enabled) }
        emitState { copy(allApps = updated) }
        legacyRepository.persistHookStates(updated)
        applyLegacyFiltersAndSort(preserveSort = true)
    }

    fun setSortMode(mode: SortMode) {
        emitState { copy(sortMode = mode) }
        applyCurrentFilters(preserveSort = true)
    }

    private fun reloadManagedApp(packageName: String): ManagedApp? =
        managedRepository.loadManagedApps().firstOrNull { it.packageName == packageName }

    private fun applyCurrentFilters(preserveSort: Boolean) {
        val current = _uiState.value ?: return
        if (current.isLegacyMode) {
            applyLegacyFiltersAndSort(preserveSort)
        } else {
            applyManagedFiltersAndSort(preserveSort)
        }
    }

    private fun applyLegacyFiltersAndSort(preserveSort: Boolean) {
        val current = _uiState.value ?: return
        val query = current.query.lowercase(Locale.getDefault())
        val filtered = current.allApps.filter { app ->
            val systemMatch = current.showSystemUi && app.isSystemApp ||
                !current.showSystemUi && !app.isSystemApp
            if (!systemMatch) return@filter false

            val hookMatch = when (current.hookFilter) {
                HookFilter.ON -> app.hookEnabled
                HookFilter.OFF -> !app.hookEnabled
                HookFilter.ALL -> true
            }
            if (!hookMatch) return@filter false

            if (query.isEmpty()) return@filter true
            app.name.lowercase(Locale.getDefault()).contains(query) ||
                app.packageName.lowercase(Locale.getDefault()).contains(query)
        }.toMutableList()

        sortLegacyList(filtered, current.sortMode)

        emitState {
            copy(
                visibleApps = filtered,
                emptyMessage = if (filtered.isEmpty()) EMPTY_FILTER else null,
            )
        }
    }

    private fun applyManagedFiltersAndSort(preserveSort: Boolean) {
        val current = _uiState.value ?: return
        val query = current.query.lowercase(Locale.getDefault())
        val filtered = current.managedApps.filter { app ->
            val filterMatch = when (current.managedFilter) {
                ManagedFilter.ENABLED -> app.interventionStatus == InterventionStatus.ENABLED
                ManagedFilter.PENDING -> app.interventionStatus == InterventionStatus.PENDING
                ManagedFilter.ALL -> true
            }
            if (!filterMatch) return@filter false

            if (query.isEmpty()) return@filter true
            app.label.lowercase(Locale.getDefault()).contains(query) ||
                app.packageName.lowercase(Locale.getDefault()).contains(query)
        }.toMutableList()

        sortManagedList(filtered, current.sortMode)

        val emptyMessage = when {
            filtered.isNotEmpty() -> null
            current.managedApps.isEmpty() -> EMPTY_MANAGED_LIST
            else -> EMPTY_FILTER
        }

        emitState {
            copy(
                visibleManagedApps = filtered,
                emptyMessage = emptyMessage,
            )
        }
    }

    private fun sortLegacyList(apps: MutableList<AppItem>, mode: SortMode) {
        when (mode) {
            SortMode.NAME_ASC -> Collections.sort(apps) { a, b -> a.name.compareTo(b.name) }
            SortMode.NAME_DESC -> Collections.sort(apps, Collections.reverseOrder { a, b -> a.name.compareTo(b.name) })
            SortMode.INSTALL_TIME_ASC -> Collections.sort(apps) { a, b -> java.lang.Long.compare(a.installTime, b.installTime) }
            SortMode.INSTALL_TIME_DESC -> Collections.sort(apps, Collections.reverseOrder { a, b -> java.lang.Long.compare(a.installTime, b.installTime) })
            SortMode.UPDATE_TIME_ASC -> Collections.sort(apps) { a, b -> java.lang.Long.compare(a.updateTime, b.updateTime) }
            SortMode.UPDATE_TIME_DESC -> Collections.sort(apps, Collections.reverseOrder { a, b -> java.lang.Long.compare(a.updateTime, b.updateTime) })
        }
    }

    private fun sortManagedList(apps: MutableList<ManagedApp>, mode: SortMode) {
        when (mode) {
            SortMode.NAME_ASC -> Collections.sort(apps) { a, b -> a.label.compareTo(b.label) }
            SortMode.NAME_DESC -> Collections.sort(apps, Collections.reverseOrder { a, b -> a.label.compareTo(b.label) })
            SortMode.INSTALL_TIME_ASC -> Collections.sort(apps) { a, b -> java.lang.Long.compare(a.installTime, b.installTime) }
            SortMode.INSTALL_TIME_DESC -> Collections.sort(apps, Collections.reverseOrder { a, b -> java.lang.Long.compare(a.installTime, b.installTime) })
            SortMode.UPDATE_TIME_ASC -> Collections.sort(apps) { a, b -> java.lang.Long.compare(a.updateTime, b.updateTime) }
            SortMode.UPDATE_TIME_DESC -> Collections.sort(apps, Collections.reverseOrder { a, b -> java.lang.Long.compare(a.updateTime, b.updateTime) })
        }
    }

    private inline fun emitState(block: ConfigUiState.() -> ConfigUiState) {
        val base = _uiState.value ?: ConfigUiState()
        _uiState.value = base.block()
    }

    class Factory(
        private val legacyRepository: AppListRepository,
        private val managedRepository: ManagedAppRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConfigViewModel(legacyRepository, managedRepository) as T
        }
    }

    companion object {
        const val EMPTY_FILTER = "filter_empty"
        const val EMPTY_MANAGED_LIST = "managed_empty"
    }
}
