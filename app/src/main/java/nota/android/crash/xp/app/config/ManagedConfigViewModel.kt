package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.PackageVisibilityHelper

internal class ManagedConfigViewModel(
    private val repository: ManagedAppRepository,
    private val visibilityRepository: PackageVisibilityRepository,
    scope: CoroutineScope,
) : BaseConfigViewModel(
    scope = scope,
    isLegacyMode = false,
    scopeMode = false,
    handleSystem = false,
    showSystemUi = false,
    packageVisibility = visibilityRepository.detectPackageVisibility(),
) {

    override fun loadApps(forceReload: Boolean) {
        val current = _uiState.value
        if (!forceReload && current.managedApps.isNotEmpty()) return

        loadWithState {
            repository.pruneUninstalled()
            val managedApps = repository.loadManagedApps()
            val visibility = visibilityRepository.detectPackageVisibility()
            emitState { copy(isLoading = false, managedApps = managedApps, packageVisibility = visibility) }
            applyFilters(preserveSort = false)
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

    override fun applyFilters(preserveSort: Boolean) = applyFilters(
        preserveSort = preserveSort,
        filter = { state ->
            AppFilterEngine.filterManagedApps(
                state.managedApps, state.query, state.managedFilter,
            )
        },
        sortExtractors = SortExtractors(
            name = { it.label },
            installTime = { it.installTime },
            updateTime = { it.updateTime },
        ),
        emptyMessage = { filtered, source ->
            when {
                filtered.isNotEmpty() -> null
                source.isEmpty() -> ConfigViewModel.EMPTY_MANAGED_LIST
                else -> ConfigViewModel.EMPTY_FILTER
            }
        },
        setState = { filtered -> copy(visibleManagedApps = filtered) },
        sourceExtractor = { it.managedApps },
    )

    private suspend fun reloadManagedApp(packageName: String): ManagedApp? =
        repository.loadManagedApps().firstOrNull { it.packageName == packageName }
}
