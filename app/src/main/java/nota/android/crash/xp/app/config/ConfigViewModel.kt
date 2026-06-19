package nota.android.crash.xp.app.config

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ConfigViewModel(
    private val repository: AppRepository,
) : ViewModel() {

    private val _uiState = MutableLiveData(ConfigUiState())
    val uiState: LiveData<ConfigUiState> = _uiState

    private var appsLoadGeneration = 0

    init {
        val legacyMode = repository.isLegacyMode()
        emitState {
            copy(
                scopeMode = repository.readScopeMode(),
                handleSystem = repository.readHandleSystem(),
                showSystemUi = repository.readShowSystemUi(),
                packageVisibility = repository.detectPackageVisibility(),
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

        viewModelScope.launch {
            try {
                if (current.isLegacyMode) {
                    val loadedApps = withContext(Dispatchers.IO) {
                        repository.loadInstalledApps()
                    }
                    val visibility = withContext(Dispatchers.IO) {
                        repository.detectPackageVisibilityAfterLoad(loadedApps.size)
                    }
                    if (loadGeneration != appsLoadGeneration) return@launch
                    emitState {
                        copy(
                            isLoading = false,
                            allApps = loadedApps,
                            packageVisibility = visibility,
                        )
                    }
                    applyLegacyFiltersAndSort(preserveSort = false)
                } else {
                    withContext(Dispatchers.IO) {
                        repository.pruneUninstalled()
                    }
                    val managedApps = withContext(Dispatchers.IO) {
                        repository.loadManagedApps()
                    }
                    val visibility = withContext(Dispatchers.IO) {
                        repository.detectPackageVisibility()
                    }
                    if (loadGeneration != appsLoadGeneration) return@launch
                    emitState {
                        copy(
                            isLoading = false,
                            managedApps = managedApps,
                            packageVisibility = visibility,
                        )
                    }
                    applyManagedFiltersAndSort(preserveSort = false)
                }
            } catch (_: Exception) {
                if (loadGeneration != appsLoadGeneration) return@launch
                emitState { copy(isLoading = false) }
            }
        }
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
        repository.setScopeMode(enabled)
        emitState { copy(scopeMode = enabled) }
    }

    fun setHandleSystem(enabled: Boolean) {
        repository.setHandleSystem(enabled)
        emitState { copy(handleSystem = enabled) }
    }

    fun setShowSystemUi(enabled: Boolean) {
        repository.setShowSystemUi(enabled)
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
            repository.persistHookStates(updated)
            applyLegacyFiltersAndSort(preserveSort = true)
        }
    }

    fun setManagedSwitch(packageName: String, enabled: Boolean) {
        val current = _uiState.value ?: return
        if (current.isLegacyMode) return

        repository.setInterventionEnabled(packageName, enabled)
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
        repository.addManagedPackages(packages)
        loadApps(forceReload = true)
    }

    fun selectAll(enabled: Boolean) {
        val current = _uiState.value ?: return
        if (!current.isLegacyMode) return
        val updated = current.allApps.map { it.copy(hookEnabled = enabled) }
        emitState { copy(allApps = updated) }
        repository.persistHookStates(updated)
        applyLegacyFiltersAndSort(preserveSort = true)
    }

    fun setSortMode(mode: SortMode) {
        emitState { copy(sortMode = mode) }
        applyCurrentFilters(preserveSort = true)
    }

    private fun reloadManagedApp(packageName: String): ManagedApp? =
        repository.loadManagedApps().firstOrNull { it.packageName == packageName }

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

        sortList(filtered, current.sortMode, { it.name }, { it.installTime }, { it.updateTime })

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

        sortList(filtered, current.sortMode, { it.label }, { it.installTime }, { it.updateTime })

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

    private fun <T> sortList(
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

    private inline fun emitState(block: ConfigUiState.() -> ConfigUiState) {
        val base = _uiState.value ?: ConfigUiState()
        _uiState.value = base.block()
    }

    class Factory(
        private val repository: AppRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConfigViewModel(repository) as T
        }
    }

    companion object {
        const val EMPTY_FILTER = "filter_empty"
        const val EMPTY_MANAGED_LIST = "managed_empty"
    }
}
