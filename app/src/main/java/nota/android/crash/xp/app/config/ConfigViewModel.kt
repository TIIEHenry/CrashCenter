package nota.android.crash.xp.app.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConfigViewModel(
    private val repository: AppRepositoryInterface,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState

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
        val current = _uiState.value
        if (!forceReload) {
            if (current.isLegacyMode && current.allApps.isNotEmpty()) return
            if (!current.isLegacyMode && current.managedApps.isNotEmpty()) return
        }

        emitState { copy(isLoading = true) }

        viewModelScope.launch {
            try {
                if (current.isLegacyMode) {
                    repository.loadInstalledApps().collect { loadedApps ->
                        val visibility = repository.detectPackageVisibilityAfterLoad(loadedApps.size)
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
                    repository.pruneUninstalled()
                    repository.loadManagedApps().collect { managedApps ->
                        val visibility = repository.detectPackageVisibility()
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
        val current = _uiState.value
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
        val current = _uiState.value
        if (current.isLegacyMode) return

        repository.setInterventionEnabled(packageName, enabled)
        viewModelScope.launch {
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
    }

    fun addManagedPackages(packages: Collection<String>) {
        if (packages.isEmpty()) return
        repository.addManagedPackages(packages)
        loadApps(forceReload = true)
    }

    fun selectAll(enabled: Boolean) {
        val current = _uiState.value
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

    private suspend fun reloadManagedApp(packageName: String): ManagedApp? =
        repository.loadManagedApps().first().firstOrNull { it.packageName == packageName }

    private fun applyCurrentFilters(preserveSort: Boolean) {
        val current = _uiState.value
        if (current.isLegacyMode) {
            applyLegacyFiltersAndSort(preserveSort)
        } else {
            applyManagedFiltersAndSort(preserveSort)
        }
    }

    private fun applyLegacyFiltersAndSort(preserveSort: Boolean) {
        val current = _uiState.value
        val filtered = AppFilterEngine.filterLegacyApps(
            current.allApps,
            current.query,
            current.hookFilter,
            current.showSystemUi,
        ).toMutableList()

        AppFilterEngine.sort(filtered, current.sortMode, { it.name }, { it.installTime }, { it.updateTime })

        emitState {
            copy(
                visibleApps = filtered,
                emptyMessage = if (filtered.isEmpty()) EMPTY_FILTER else null,
            )
        }
    }

    private fun applyManagedFiltersAndSort(preserveSort: Boolean) {
        val current = _uiState.value
        val filtered = AppFilterEngine.filterManagedApps(
            current.managedApps,
            current.query,
            current.managedFilter,
        ).toMutableList()

        AppFilterEngine.sort(filtered, current.sortMode, { it.label }, { it.installTime }, { it.updateTime })

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

    private fun emitState(block: ConfigUiState.() -> ConfigUiState) {
        _uiState.value = _uiState.value.block()
    }

    companion object {
        const val EMPTY_FILTER = "filter_empty"
        const val EMPTY_MANAGED_LIST = "managed_empty"
    }
}
