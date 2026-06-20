package nota.android.crash.xp.app.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
}

class AddManagedAppViewModel(
    private val repository: AppRepositoryInterface,
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
                repository.loadPickableApps().collect { loaded ->
                    allApps = loaded
                    _uiState.value = AddManagedAppUiState.Success(apps = loaded)
                }
            } catch (_: Exception) {
                _uiState.value = AddManagedAppUiState.Success(apps = emptyList())
            }
        }
    }

    fun setQuery(query: String) {
        val current = _uiState.value as? AddManagedAppUiState.Success ?: return
        val normalized = query.lowercase(java.util.Locale.getDefault())
        val visible = allApps.filter { app ->
            if (normalized.isEmpty()) return@filter true
            app.label.lowercase(java.util.Locale.getDefault()).contains(normalized) ||
                app.packageName.lowercase(java.util.Locale.getDefault()).contains(normalized)
        }
        _uiState.value = current.copy(apps = visible, query = query)
    }

    class Factory(
        private val repository: AppRepositoryInterface,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return AddManagedAppViewModel(repository) as T
        }
    }
}
