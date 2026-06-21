package nota.android.crash.xp.app.observe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.CrashLogRepository

class CrashHistoryViewModel(
    private val repository: CrashLogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrashHistoryUiState())
    val uiState: StateFlow<CrashHistoryUiState> = _uiState

    internal var loadJob: Job? = null

    val pagingData: Flow<PagingData<CrashEvent>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { CrashEventPagingSource(repository, CrashFilter()) },
    ).flow.cachedIn(viewModelScope)

    fun loadEvents() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val count = withContext(ioDispatcher) {
                    repository.getCount(CrashFilter())
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    eventCount = count,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message,
                )
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 50
    }
}
