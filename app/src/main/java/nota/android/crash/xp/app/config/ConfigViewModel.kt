package nota.android.crash.xp.app.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow

class ConfigViewModel(
    legacyRepository: LegacyAppRepository,
    managedRepository: ManagedAppRepository,
    visibilityRepository: PackageVisibilityRepository,
) : ViewModel() {

    private val delegate: ConfigViewModelDelegate = createDelegate(
        legacyRepository,
        managedRepository,
        visibilityRepository,
    )

    val uiState: StateFlow<ConfigUiState> = delegate.uiState

    fun loadApps(forceReload: Boolean = false) = delegate.loadApps(forceReload)
    fun setQuery(query: String) = delegate.setQuery(query)
    fun setHookFilter(filter: HookFilter) = delegate.setHookFilter(filter)
    fun setManagedFilter(filter: ManagedFilter) = delegate.setManagedFilter(filter)
    fun setScopeMode(enabled: Boolean) = delegate.setScopeMode(enabled)
    fun setHandleSystem(enabled: Boolean) = delegate.setHandleSystem(enabled)
    fun setShowSystemUi(enabled: Boolean) = delegate.setShowSystemUi(enabled)
    fun toggleApp(packageName: String) = delegate.toggleApp(packageName)
    fun setManagedSwitch(packageName: String, enabled: Boolean) = delegate.setManagedSwitch(packageName, enabled)
    fun addManagedPackages(packages: Collection<String>) = delegate.addManagedPackages(packages)
    fun selectAll(enabled: Boolean) = delegate.selectAll(enabled)
    fun setSortMode(mode: SortMode) = delegate.setSortMode(mode)

    private fun createDelegate(
        legacyRepository: LegacyAppRepository,
        managedRepository: ManagedAppRepository,
        visibilityRepository: PackageVisibilityRepository,
    ): ConfigViewModelDelegate {
        return if (managedRepository.isLegacyMode()) {
            LegacyConfigViewModel(legacyRepository, visibilityRepository, viewModelScope)
        } else {
            ManagedConfigViewModel(managedRepository, visibilityRepository, viewModelScope)
        }
    }

    companion object {
        const val EMPTY_FILTER = "filter_empty"
        const val EMPTY_MANAGED_LIST = "managed_empty"
    }
}

internal interface ConfigViewModelDelegate {
    val uiState: StateFlow<ConfigUiState>
    fun loadApps(forceReload: Boolean = false)
    fun setQuery(query: String)
    fun setHookFilter(filter: HookFilter)
    fun setManagedFilter(filter: ManagedFilter)
    fun setScopeMode(enabled: Boolean)
    fun setHandleSystem(enabled: Boolean)
    fun setShowSystemUi(enabled: Boolean)
    fun toggleApp(packageName: String)
    fun setManagedSwitch(packageName: String, enabled: Boolean)
    fun addManagedPackages(packages: Collection<String>)
    fun selectAll(enabled: Boolean)
    fun setSortMode(mode: SortMode)
}
