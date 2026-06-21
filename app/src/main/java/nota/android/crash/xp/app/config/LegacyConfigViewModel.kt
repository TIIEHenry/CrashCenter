package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.PackageVisibilityHelper

internal class LegacyConfigViewModel(
    private val repository: LegacyAppRepository,
    private val visibilityRepository: PackageVisibilityRepository,
    scope: CoroutineScope,
) : BaseConfigViewModel(
    scope = scope,
    isLegacyMode = true,
    scopeMode = repository.readScopeMode(),
    handleSystem = repository.readHandleSystem(),
    showSystemUi = repository.readShowSystemUi(),
    packageVisibility = visibilityRepository.detectPackageVisibility(),
) {

    override fun loadApps(forceReload: Boolean) {
        val current = _uiState.value
        if (!forceReload && current.allApps.isNotEmpty()) return

        emitState { copy(isLoading = true) }

        scope.launch {
            try {
                val loadedApps = repository.loadInstalledApps()
                val visibility = visibilityRepository.detectPackageVisibilityAfterLoad(loadedApps.size)
                emitState {
                    copy(
                        isLoading = false,
                        allApps = loadedApps,
                        packageVisibility = visibility,
                    )
                }
                applyFilters(preserveSort = false)
            } catch (_: Exception) {
                emitState { copy(isLoading = false) }
            }
        }
    }

    override fun setScopeMode(enabled: Boolean) {
        repository.setScopeMode(enabled)
        super.setScopeMode(enabled)
    }

    override fun setHandleSystem(enabled: Boolean) {
        repository.setHandleSystem(enabled)
        super.setHandleSystem(enabled)
    }

    override fun setShowSystemUi(enabled: Boolean) {
        repository.setShowSystemUi(enabled)
        super.setShowSystemUi(enabled)
    }

    override fun setHookFilter(filter: HookFilter) {
        emitState { copy(hookFilter = filter) }
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

    override fun selectAll(enabled: Boolean) {
        val updated = _uiState.value.allApps.map { it.copy(hookEnabled = enabled) }
        emitState { copy(allApps = updated) }
        repository.persistHookStates(updated)
        applyFilters(preserveSort = true)
    }

    override fun applyFilters(preserveSort: Boolean) {
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
}
