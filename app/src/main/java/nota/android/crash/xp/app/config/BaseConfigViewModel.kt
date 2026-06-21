package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.PackageVisibilityHelper

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

    protected fun <T> applyFilters(
        preserveSort: Boolean,
        filter: (ConfigUiState) -> List<T>,
        sortExtractors: SortExtractors<T>,
        emptyMessage: (List<T>) -> String?,
        setState: ConfigUiState.(List<T>) -> ConfigUiState,
    ) {
        val current = _uiState.value
        val filtered = filter(current).toMutableList()
        AppFilterEngine.sort(
            filtered, current.sortMode,
            sortExtractors.name, sortExtractors.installTime, sortExtractors.updateTime,
        )
        emitState { setState(filtered) }
        emitState { copy(emptyMessage = emptyMessage(filtered)) }
    }

    protected fun <T> applyFilters(
        preserveSort: Boolean,
        filter: (ConfigUiState) -> List<T>,
        sortExtractors: SortExtractors<T>,
        emptyMessage: (List<T>, List<T>) -> String?,
        setState: ConfigUiState.(List<T>) -> ConfigUiState,
        sourceExtractor: (ConfigUiState) -> List<T>,
    ) {
        val current = _uiState.value
        val filtered = filter(current).toMutableList()
        val source = sourceExtractor(current)
        AppFilterEngine.sort(
            filtered, current.sortMode,
            sortExtractors.name, sortExtractors.installTime, sortExtractors.updateTime,
        )
        emitState { setState(filtered) }
        emitState { copy(emptyMessage = emptyMessage(filtered, source)) }
    }

    protected fun loadWithState(block: suspend () -> Unit) {
        emitState { copy(isLoading = true) }
        scope.launch {
            try {
                block()
            } catch (_: Exception) {
                emitState { copy(isLoading = false) }
            }
        }
    }

    abstract fun applyFilters(preserveSort: Boolean)
}

internal data class SortExtractors<T>(
    val name: (T) -> String,
    val installTime: (T) -> Long,
    val updateTime: (T) -> Long,
)
