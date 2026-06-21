package nota.android.crash.xp.app.config

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.common.BaseFlowViewModel

sealed class AddManagedAppUiState {
    data object Loading : AddManagedAppUiState()
    data class Success(
        val apps: List<PickableApp>,
        val query: String = "",
    ) : AddManagedAppUiState()

    data class Error(
        val message: String,
    ) : AddManagedAppUiState()
}

class AddManagedAppViewModel(
    private val repository: ManagedAppRepository,
) : BaseFlowViewModel<AddManagedAppUiState>(AddManagedAppUiState.Loading) {

    private var allApps: List<PickableApp> = emptyList()

    init {
        loadPickableApps()
    }

    private fun loadPickableApps() {
        launchWithErrorHandling(
            scope = viewModelScope,
            onError = { e ->
                emitState { AddManagedAppUiState.Error(e.message ?: "Unknown error") }
            },
        ) {
            val loaded = repository.loadPickableApps()
            allApps = loaded
            emitState { AddManagedAppUiState.Success(apps = loaded) }
        }
    }

    fun setQuery(query: String) {
        val current = _uiState.value as? AddManagedAppUiState.Success ?: return
        val visible = AppFilterEngine.filterByQuery(
            items = allApps,
            query = query,
            labelExtractor = { it.label },
            packageNameExtractor = { it.packageName },
        )
        emitState { (this as AddManagedAppUiState.Success).copy(apps = visible, query = query) }
    }
}
