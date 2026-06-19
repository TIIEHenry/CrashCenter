package nota.android.crash.xp.app.observe

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.CrashLogRepository

class CrashHistoryViewModel(
    private val repository: CrashLogRepository,
) : ViewModel() {

    private val _uiState = MutableLiveData(CrashHistoryUiState())
    val uiState: LiveData<CrashHistoryUiState> = _uiState

    private var loadGeneration = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    fun loadEvents(forceReload: Boolean = false) {
        val current = _uiState.value ?: CrashHistoryUiState()
        if (!forceReload && !current.isLoading && current.events.isNotEmpty()) {
            return
        }

        val generation = ++loadGeneration
        emitState { copy(isLoading = true) }

        Thread {
            val events = try {
                repository.getAll(CrashFilter(), Int.MAX_VALUE, 0)
            } catch (_: Exception) {
                emptyList()
            }
            mainHandler.post {
                if (generation != loadGeneration) return@post
                emitState {
                    copy(
                        isLoading = false,
                        events = events,
                        eventCount = events.size,
                    )
                }
            }
        }.start()
    }

    private inline fun emitState(block: CrashHistoryUiState.() -> CrashHistoryUiState) {
        val base = _uiState.value ?: CrashHistoryUiState()
        _uiState.value = base.block()
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
