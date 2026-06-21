package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    abstract fun applyFilters(preserveSort: Boolean)
}
