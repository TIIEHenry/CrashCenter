package nota.android.crash.xp.app.observe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.xp.app.data.CrashDetailLoader
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CrashDetailUiState>(CrashDetailUiState.Loading)
    val uiState: StateFlow<CrashDetailUiState> = _uiState

    init {
        loadCrashDetail()
    }

    private fun loadCrashDetail() {
        viewModelScope.launch {
            val event = withContext(ioDispatcher) {
                repository.getById(crashId)
            }
            val stackTrace = event?.let { CrashDetailLoader.stackTraceFrom(it) }
                ?: "Crash detail not found: $crashId"
            val title = event?.shortExceptionClass
                ?: CrashDetailLoader.titleFromStackTrace(stackTrace)
                ?: "Crash Info"
            _uiState.value = CrashDetailUiState.Success(title, stackTrace)
        }
    }
}
