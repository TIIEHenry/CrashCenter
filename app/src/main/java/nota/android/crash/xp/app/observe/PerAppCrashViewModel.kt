package nota.android.crash.xp.app.observe

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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

    private val repository = repository
    private val aggregator = StatsAggregator(repository)
    private val baseFilter = CrashFilter(packageName = packageName, exceptionClass = exceptionClass)
    private val _query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingData: Flow<PagingData<CrashEvent>> = _query.flatMapLatest { query ->
        val filter = baseFilter.copy(query = query.takeIf { it.isNotBlank() })
        Pager(
            config = PagingConfig(
                pageSize = CrashHistoryViewModel.PAGE_SIZE,
                prefetchDistance = CrashHistoryViewModel.PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { CrashEventPagingSource(repository, filter) },
        ).flow.cachedIn(viewModelScope)
    }

    fun setFilterQuery(query: String) {
        _query.value = query
    }

    fun loadSummary() {
        loadWithState(viewModelScope) {
            val summary = withContext(ioDispatcher) {
                if (packageName != null) {
                    aggregator.computePerAppStats(packageName)
                } else {
                    aggregator.computeFilteredStats(baseFilter)
                }
            }
            emitState { copy(isLoading = false, summary = summary) }
        }
    }

    fun clearRecords() {
        val pkg = packageName ?: return
        launchWithErrorHandling(
            scope = viewModelScope,
            onError = { e -> emitState { copy(errorMessage = e.message) } },
        ) {
            withContext(ioDispatcher) { repository.deleteByPackage(pkg) }
            emitState { copy(historyCleared = historyCleared + 1) }
        }
    }
}