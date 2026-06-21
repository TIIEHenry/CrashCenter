package nota.android.crash.xp.app.observe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.xp.app.common.BaseFlowViewModel
import nota.android.crash.xp.app.common.safeLog
import nota.android.crash.xp.app.data.CrashDetailLoader
import nota.android.crash.xp.app.data.CrashLogRepository

sealed class CrashDetailUiState {
    data object Loading : CrashDetailUiState()
    data class Success(
        val title: String,
        val stackTrace: String,
    ) : CrashDetailUiState()

    data class Error(
        val message: String,
    ) : CrashDetailUiState()
}

class CrashDetailViewModel(
    private val repository: CrashLogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    savedStateHandle: SavedStateHandle,
) : BaseFlowViewModel<CrashDetailUiState>(CrashDetailUiState.Loading) {

    private val crashId: String = savedStateHandle.get<String>(ARG_CRASH_ID).orEmpty()

    init {
        loadCrashDetail()
    }

    private fun loadCrashDetail() {
        viewModelScope.launch {
            try {
                val event = withContext(ioDispatcher) {
                    repository.getById(crashId)
                }
                val stackTrace = event?.let { CrashDetailLoader.stackTraceFrom(it) }
                    ?: "Crash detail not found: $crashId"
                val title = event?.shortExceptionClass
                    ?: CrashDetailLoader.titleFromStackTrace(stackTrace)
                    ?: "Crash Info"
                emitState { CrashDetailUiState.Success(title, stackTrace) }
            } catch (e: Exception) {
                safeLog("CrashDetailViewModel", "loadCrashDetail failed", e)
                emitState {
                    CrashDetailUiState.Error(
                        message = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_CRASH_ID = CrashDetailBottomSheet.EXTRA_CRASH_ID
    }
}
