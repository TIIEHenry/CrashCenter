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
import nota.android.crash.xp.app.data.StatsAggregator

class PerAppCrashViewModel(
    private val packageName: String?,
    exceptionClass: String? = null,
    repository: CrashLogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseFlowViewModel<PerAppCrashUiState>(PerAppCrashUiState()) {

    private val aggregator = StatsAggregator(repository)
    private val filter = CrashFilter(packageName = packageName, exceptionClass = exceptionClass)

    val pagingData: Flow<PagingData<CrashEvent>> = Pager(
        config = PagingConfig(
            pageSize = CrashHistoryViewModel.PAGE_SIZE,
            prefetchDistance = CrashHistoryViewModel.PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { CrashEventPagingSource(repository, filter) },
    ).flow.cachedIn(viewModelScope)

    fun loadSummary() {
        loadWithState(viewModelScope) {
            val summary = withContext(ioDispatcher) {
                if (packageName != null) {
                    aggregator.computePerAppStats(packageName)
                } else {
                    aggregator.computeFilteredStats(filter)
                }
            }
            emitState { copy(isLoading = false, summary = summary) }
        }
    }
}
