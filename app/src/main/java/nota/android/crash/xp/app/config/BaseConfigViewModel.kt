package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.common.BaseFlowViewModel

internal abstract class BaseConfigViewModel(
    protected val scope: CoroutineScope,
    isLegacyMode: Boolean,
    scopeMode: Boolean,
    handleSystem: Boolean,
    showSystemUi: Boolean,
    packageVisibility: PackageVisibilityHelper.Status,
) : BaseFlowViewModel<ConfigUiState>(
    ConfigUiState(
        isLegacyMode = isLegacyMode,
        scopeMode = scopeMode,
        handleSystem = handleSystem,
        showSystemUi = showSystemUi,
        packageVisibility = packageVisibility,
    )
), ConfigViewModelDelegate {

    override fun setQuery(query: String) {
        emitState { copy(query = query) }
        applyFilters(preserveSort = true)
    }

    override fun setScopeMode(enabled: Boolean) {
        emitState { copy(scopeMode = enabled) }
    }

    override fun setHandleSystem(enabled: Boolean) {
        emitState { copy(handleSystem = enabled) }
    }

    override fun setShowSystemUi(enabled: Boolean) {
        emitState { copy(showSystemUi = enabled) }
        applyFilters(preserveSort = true)
    }

    override fun setSortMode(mode: SortMode) {
        emitState { copy(sortMode = mode) }
        applyFilters(preserveSort = true)
    }

    override fun setHookFilter(filter: HookFilter) {
        // No-op by default; override in legacy
    }

    override fun setManagedFilter(filter: ManagedFilter) {
        // No-op by default; override in managed
    }

    override fun toggleApp(packageName: String) {
        // No-op by default; override in legacy
    }

    override fun setManagedSwitch(packageName: String, enabled: Boolean) {
        // No-op by default; override in managed
    }

    override fun addManagedPackages(packages: Collection<String>) {
        // No-op by default; override in managed
    }

    override fun selectAll(enabled: Boolean) {
        // No-op by default; override in legacy
    }

    protected fun <T : AppListItem> applyFilters(
        preserveSort: Boolean,
        filter: (ConfigUiState) -> List<T>,
        emptyMessage: (List<T>, List<T>) -> String?,
        setState: ConfigUiState.(List<T>) -> ConfigUiState,
        sourceExtractor: (ConfigUiState) -> List<T>,
    ) {
        val current = _uiState.value
        val filtered = filter(current).toMutableList()
        val source = sourceExtractor(current)
        AppFilterEngine.sort(filtered, current.sortMode)
        emitState { setState(filtered) }
        emitState { copy(emptyMessage = emptyMessage(filtered, source)) }
    }

    abstract fun applyFilters(preserveSort: Boolean)
}

