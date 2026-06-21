package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.common.safeLog

internal abstract class BaseConfigViewModel(
    protected val scope: CoroutineScope,
    isLegacyMode: Boolean,
    scopeMode: Boolean,
    handleSystem: Boolean,
    showSystemUi: Boolean,
    packageVisibility: PackageVisibilityHelper.Status,
) : ConfigViewModelDelegate {

    protected val _uiState = MutableStateFlow(
        ConfigUiState(
            isLegacyMode = isLegacyMode,
            scopeMode = scopeMode,
            handleSystem = handleSystem,
            showSystemUi = showSystemUi,
            packageVisibility = packageVisibility,
        )
    )
    override val uiState: StateFlow<ConfigUiState> = _uiState

    protected fun emitState(block: ConfigUiState.() -> ConfigUiState) {
        _uiState.value = _uiState.value.block()
    }

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

    override fun clearError() {
        emitState { copy(errorMessage = null) }
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

    protected fun loadWithState(block: suspend () -> Unit) {
        emitState { copy(isLoading = true) }
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                safeLog("BaseConfigViewModel", "load failed", e)
                emitState { copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    abstract fun applyFilters(preserveSort: Boolean)
}

