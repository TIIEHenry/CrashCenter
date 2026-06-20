package nota.android.crash.xp.app.observe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.CrashEvent
import nota.android.crash.xp.app.data.CrashLogRepository

class CrashHistoryViewModel(
    private val repository: CrashLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrashHistoryUiState())
    val uiState: StateFlow<CrashHistoryUiState> = _uiState

    val pagingData: Flow<PagingData<CrashEvent>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { CrashEventPagingSource(repository, CrashFilter()) },
    ).flow.cachedIn(viewModelScope)

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val count = repository.getCount(CrashFilter())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    eventCount = count,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    class Factory(
        private val repository: CrashLogRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CrashHistoryViewModel(repository) as T
        }
    }

    companion object {
        const val PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 50
    }
}
