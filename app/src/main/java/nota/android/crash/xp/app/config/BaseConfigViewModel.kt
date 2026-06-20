package nota.android.crash.xp.app.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal abstract class BaseConfigViewModel(
    protected val repository: AppRepositoryInterface,
    protected val scope: CoroutineScope,
    isLegacyMode: Boolean,
) : ConfigViewModelDelegate {

    protected val _uiState = MutableStateFlow(
        ConfigUiState(
            isLegacyMode = isLegacyMode,
            scopeMode = repository.readScopeMode(),
            handleSystem = repository.readHandleSystem(),
            showSystemUi = repository.readShowSystemUi(),
            packageVisibility = repository.detectPackageVisibility(),
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
