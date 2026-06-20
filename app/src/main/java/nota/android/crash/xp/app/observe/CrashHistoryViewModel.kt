package nota.android.crash.xp.app.observe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.CrashLogRepository

class CrashHistoryViewModel(
    private val repository: CrashLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrashHistoryUiState())
    val uiState: StateFlow<CrashHistoryUiState> = _uiState

    private var loadGeneration = 0

    fun loadEvents(forceReload: Boolean = false) {
        val current = _uiState.value
        if (!forceReload && !current.isLoading && current.events.isNotEmpty()) {
            return
        }

        val generation = ++loadGeneration
        emitState { copy(isLoading = true) }

        viewModelScope.launch {
            val events = withContext(Dispatchers.IO) {
                try {
                    repository.getAll(CrashFilter(), Int.MAX_VALUE, 0)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            if (generation != loadGeneration) return@launch
            emitState {
                copy(
                    isLoading = false,
                    events = events,
                    eventCount = events.size,
                )
            }
        }
    }

    private inline fun emitState(block: CrashHistoryUiState.() -> CrashHistoryUiState) {
        _uiState.value = _uiState.value.block()
    }

    class Factory(
        private val repository: CrashLogRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CrashHistoryViewModel(repository) as T
        }
    }
}
