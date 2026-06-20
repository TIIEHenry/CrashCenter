package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class ManagedConfigViewModel(
    repository: AppRepositoryInterface,
    scope: CoroutineScope,
) : BaseConfigViewModel(repository, scope, isLegacyMode = false) {

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

    override fun setManagedFilter(filter: ManagedFilter) {
        emitState { copy(managedFilter = filter) }
        applyFilters(preserveSort = true)
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

    override fun applyFilters(preserveSort: Boolean) {
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

    private suspend fun reloadManagedApp(packageName: String): ManagedApp? =
        repository.loadManagedApps().first().firstOrNull { it.packageName == packageName }
}
