package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class LegacyConfigViewModel(
    private val repository: AppRepositoryInterface,
    private val scope: CoroutineScope,
) : ConfigViewModelDelegate {

    private val _uiState = MutableStateFlow(
        ConfigUiState(
            isLegacyMode = true,
            scopeMode = repository.readScopeMode(),
            handleSystem = repository.readHandleSystem(),
            showSystemUi = repository.readShowSystemUi(),
            packageVisibility = repository.detectPackageVisibility(),
        )
    )
    override val uiState: StateFlow<ConfigUiState> = _uiState

    override fun loadApps(forceReload: Boolean) {
        val current = _uiState.value
        if (!forceReload && current.allApps.isNotEmpty()) return

        emitState { copy(isLoading = true) }

        scope.launch {
            try {
                repository.loadInstalledApps().collect { loadedApps ->
                    val visibility = repository.detectPackageVisibilityAfterLoad(loadedApps.size)
                    emitState {
                        copy(
                            isLoading = false,
                            allApps = loadedApps,
                            packageVisibility = visibility,
                        )
                    }
                    applyFilters(preserveSort = false)
                }
            } catch (_: Exception) {
                emitState { copy(isLoading = false) }
            }
        }
    }

    override fun setQuery(query: String) {
        emitState { copy(query = query) }
        applyFilters(preserveSort = true)
    }

    override fun setHookFilter(filter: HookFilter) {
        emitState { copy(hookFilter = filter) }
        applyFilters(preserveSort = true)
    }

    override fun setManagedFilter(filter: ManagedFilter) {
        // No-op in legacy mode
    }

    override fun setScopeMode(enabled: Boolean) {
        repository.setScopeMode(enabled)
        emitState { copy(scopeMode = enabled) }
    }

    override fun setHandleSystem(enabled: Boolean) {
        repository.setHandleSystem(enabled)
        emitState { copy(handleSystem = enabled) }
    }

    override fun setShowSystemUi(enabled: Boolean) {
        repository.setShowSystemUi(enabled)
        emitState { copy(showSystemUi = enabled) }
        applyFilters(preserveSort = true)
    }

    override fun toggleApp(packageName: String) {
        val current = _uiState.value
        val updated = current.allApps.map { app ->
            if (app.packageName == packageName) {
                app.copy(hookEnabled = !app.hookEnabled)
            } else {
                app
            }
        }
        emitState { copy(allApps = updated) }
        repository.persistHookStates(updated)
        applyFilters(preserveSort = true)
    }

    override fun setManagedSwitch(packageName: String, enabled: Boolean) {
        // No-op in legacy mode
    }

    override fun addManagedPackages(packages: Collection<String>) {
        // No-op in legacy mode
    }

    override fun selectAll(enabled: Boolean) {
        val updated = _uiState.value.allApps.map { it.copy(hookEnabled = enabled) }
        emitState { copy(allApps = updated) }
        repository.persistHookStates(updated)
        applyFilters(preserveSort = true)
    }

    override fun setSortMode(mode: SortMode) {
        emitState { copy(sortMode = mode) }
        applyFilters(preserveSort = true)
    }

    private fun applyFilters(preserveSort: Boolean) {
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
                emptyMessage = if (filtered.isEmpty()) ConfigViewModel.EMPTY_FILTER else null,
            )
        }
    }

    private fun emitState(block: ConfigUiState.() -> ConfigUiState) {
        _uiState.value = _uiState.value.block()
    }
}
