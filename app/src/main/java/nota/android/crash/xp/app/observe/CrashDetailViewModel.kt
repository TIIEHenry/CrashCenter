package nota.android.crash.xp.app.observe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.data.CrashDetailLoader
import nota.android.crash.xp.app.data.CrashEvent
import nota.android.crash.xp.app.data.CrashLogRepository

sealed class CrashDetailUiState {
    data object Loading : CrashDetailUiState()
    data class Success(
        val title: String,
        val stackTrace: String,
    ) : CrashDetailUiState()
}

class CrashDetailViewModel(
    private val crashId: String,
    private val repository: CrashLogRepository,
    private val contextProvider: () -> android.content.Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CrashDetailUiState>(CrashDetailUiState.Loading)
    val uiState: StateFlow<CrashDetailUiState> = _uiState

    init {
        loadCrashDetail()
    }

    private fun loadCrashDetail() {
        viewModelScope.launch {
            val event = repository.getById(crashId)
            val stackTrace = CrashDetailLoader.loadStackTraceById(contextProvider(), crashId)
                ?: "Crash detail not found: $crashId"
            val title = event?.shortExceptionClass
                ?: titleFromStackTrace(stackTrace)
                ?: "Crash Info"
            _uiState.value = CrashDetailUiState.Success(title, stackTrace)
        }
    }

    private fun titleFromStackTrace(stackTrace: String): String? {
        val firstLine = stackTrace.lineSequence().firstOrNull()?.trim().orEmpty()
        if (firstLine.isEmpty()) return null
        val exceptionToken = firstLine.substringBefore(':').trim()
        return exceptionToken.substringAfterLast('.').ifBlank { exceptionToken }
    }

    class Factory(
        private val crashId: String,
        private val repository: CrashLogRepository,
        private val contextProvider: () -> android.content.Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CrashDetailViewModel(crashId, repository, contextProvider) as T
        }
    }
}
