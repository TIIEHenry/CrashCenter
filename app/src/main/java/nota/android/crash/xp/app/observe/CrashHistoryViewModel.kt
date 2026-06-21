package nota.android.crash.xp.app.observe

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.common.BaseFlowViewModel
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.CrashLogRepository

class CrashHistoryViewModel(
    private val repository: CrashLogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseFlowViewModel<CrashHistoryUiState>(CrashHistoryUiState()) {

    val pagingData: Flow<PagingData<CrashEvent>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { CrashEventPagingSource(repository, CrashFilter()) },
    ).flow.cachedIn(viewModelScope)

    fun loadEvents() {
        loadWithState(viewModelScope) {
            val count = withContext(ioDispatcher) { repository.getCount(CrashFilter()) }
            emitState { copy(isLoading = false, eventCount = count) }
        }
    }

    companion object {
        const val PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 50
    }
}
