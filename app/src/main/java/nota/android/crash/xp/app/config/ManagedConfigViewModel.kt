package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class ManagedConfigViewModel(
    private val repository: AppRepositoryInterface,
    private val scope: CoroutineScope,
) : ConfigViewModelDelegate {

    private val _uiState = MutableStateFlow(
        ConfigUiState(
            isLegacyMode = false,
            scopeMode = repository.readScopeMode(),
            handleSystem = repository.readHandleSystem(),
            showSystemUi = repository.readShowSystemUi(),
            packageVisibility = repository.detectPackageVisibility(),
        )
    )
    override val uiState: StateFlow<ConfigUiState> = _uiState

    override fun loadApps(forceReload: Boolean) {
        val current = _uiState.value
        if (!forceReload && current.managedApps.isNotEmpty()) return

        emitState { copy(isLoading = true) }

        scope.launch {
            try {
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
        // No-op in managed mode
    }

    override fun setManagedFilter(filter: ManagedFilter) {
        emitState { copy(managedFilter = filter) }
        applyFilters(preserveSort = true)
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
        // No-op in managed mode
    }

    override fun setManagedSwitch(packageName: String, enabled: Boolean) {
        repository.setInterventionEnabled(packageName, enabled)
        scope.launch {
            val current = _uiState.value
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
            applyFilters(preserveSort = true)
        }
    }

    override fun addManagedPackages(packages: Collection<String>) {
        if (packages.isEmpty()) return
        repository.addManagedPackages(packages)
        loadApps(forceReload = true)
    }

    override fun selectAll(enabled: Boolean) {
        // No-op in managed mode
    }

    override fun setSortMode(mode: SortMode) {
        emitState { copy(sortMode = mode) }
        applyFilters(preserveSort = true)
    }

    private suspend fun reloadManagedApp(packageName: String): ManagedApp? =
        repository.loadManagedApps().first().firstOrNull { it.packageName == packageName }

    private fun applyFilters(preserveSort: Boolean) {
        val current = _uiState.value
        val filtered = AppFilterEngine.filterManagedApps(
            current.managedApps,
            current.query,
            current.managedFilter,
        ).toMutableList()

        AppFilterEngine.sort(filtered, current.sortMode, { it.label }, { it.installTime }, { it.updateTime })

        val emptyMessage = when {
            filtered.isNotEmpty() -> null
            current.managedApps.isEmpty() -> ConfigViewModel.EMPTY_MANAGED_LIST
            else -> ConfigViewModel.EMPTY_FILTER
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
}
