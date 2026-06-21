package nota.android.crash.xp.app.config

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddManagedAppUiState>(AddManagedAppUiState.Loading)
    val uiState: StateFlow<AddManagedAppUiState> = _uiState

    private var allApps: List<PickableApp> = emptyList()

    init {
        loadPickableApps()
    }

    private fun loadPickableApps() {
        viewModelScope.launch {
            try {
                val loaded = repository.loadPickableApps()
                allApps = loaded
                _uiState.value = AddManagedAppUiState.Success(apps = loaded)
            } catch (e: Exception) {
                try { Log.w("AddManagedAppViewModel", "loadPickableApps failed", e) } catch (_: Throwable) {}
                _uiState.value = AddManagedAppUiState.Error(e.message ?: "Unknown error")
            }
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
        _uiState.value = current.copy(apps = visible, query = query)
    }
}
