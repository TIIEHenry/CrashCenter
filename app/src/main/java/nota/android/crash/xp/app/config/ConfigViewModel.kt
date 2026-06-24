package nota.android.crash.xp.app.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.common.BaseFlowViewModel

class ConfigViewModel(
    private val repository: ManagedAppRepository,
    private val visibilityRepository: PackageVisibilityRepository,
) : BaseFlowViewModel<ConfigUiState>(
    ConfigUiState(
        handleSystem = repository.readHandleSystem(),
        showSystemUi = repository.readShowSystemUi(),
        packageVisibility = visibilityRepository.detectPackageVisibility(),
    ),
) {

    fun loadApps(forceReload: Boolean = false) {
        val current = _uiState.value
        if (!forceReload && current.apps.isNotEmpty()) return

        loadWithState(viewModelScope) {
            val apps = repository.loadInstalledApps()
            val visibility = visibilityRepository.detectPackageVisibility()
            emitState { copy(isLoading = false, apps = apps, packageVisibility = visibility) }
            applyFilters(preserveSort = false)
        }
    }

    fun setQuery(query: String) {
        emitState { copy(query = query) }
        applyFilters(preserveSort = true)
    }

    fun setHandleSystem(enabled: Boolean) {
        repository.setHandleSystem(enabled)
        emitState { copy(handleSystem = enabled) }
        applyFilters(preserveSort = true)
    }

    fun setShowSystemUi(enabled: Boolean) {
        repository.setShowSystemUi(enabled)
        emitState { copy(showSystemUi = enabled) }
        applyFilters(preserveSort = true)
    }

    fun setSortMode(mode: SortMode) {
        emitState { copy(sortMode = mode) }
        applyFilters(preserveSort = true)
    }

    fun setInterceptFilter(filter: InterceptFilter) {
        emitState { copy(interceptFilter = filter) }
        applyFilters(preserveSort = true)
    }

    fun toggleIntercept(packageName: String) {
        val current = _uiState.value.apps.find { it.packageName == packageName } ?: return
        val enabled = !current.interceptEnabled
        repository.setInterceptEnabled(packageName, enabled)
        val updated = _uiState.value.apps.map { app ->
            if (app.packageName == packageName) app.copy(interceptEnabled = enabled) else app
        }
        emitState { copy(apps = updated) }
        applyFilters(preserveSort = true)
    }

    private fun applyFilters(preserveSort: Boolean) {
        val current = _uiState.value
        val filtered = AppFilterEngine.sort(
            current.apps.filter { app ->
                if (!current.showSystemUi && app.isSystem) return@filter false
                when (current.interceptFilter) {
                    InterceptFilter.ENABLED -> if (!app.interceptEnabled) return@filter false
                    InterceptFilter.DISABLED -> if (app.interceptEnabled) return@filter false
                    InterceptFilter.ALL -> {}
                }
                val q = current.query.trim().lowercase()
                if (q.isNotEmpty()) {
                    app.label.lowercase().contains(q) || app.packageName.lowercase().contains(q)
                } else true
            },
            current.sortMode,
        )
        val emptyMsg = when {
            filtered.isNotEmpty() -> null
            current.apps.isEmpty() -> EMPTY_LIST
            else -> EMPTY_FILTER
        }
        emitState { copy(visibleApps = filtered, emptyMessage = emptyMsg) }
    }

    companion object {
        const val EMPTY_LIST = "empty_list"
        const val EMPTY_FILTER = "filter_empty"
    }
}
