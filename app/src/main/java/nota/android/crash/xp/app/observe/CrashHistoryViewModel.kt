package nota.android.crash.xp.app.observe

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.common.BaseFlowViewModel
import nota.android.crash.xp.app.common.safeLog
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.CrashLogRepository

class CrashHistoryViewModel(
    private val repository: CrashLogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseFlowViewModel<CrashHistoryUiState>(CrashHistoryUiState()) {

    private val currentFilter = MutableStateFlow(CrashFilter())

    val pagingData: Flow<PagingData<CrashEvent>> = currentFilter
        .flatMapLatest { filter ->
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    prefetchDistance = PREFETCH_DISTANCE,
                    enablePlaceholders = false,
                ),
                pagingSourceFactory = { CrashEventPagingSource(repository, filter) },
            ).flow
        }
        .cachedIn(viewModelScope)

    fun setFilter(filter: CrashFilter) {
        currentFilter.value = filter
        val active = filter.takeIf { it != CrashFilter() }
        emitState { copy(activeFilter = active) }
        loadEvents()
    }

    fun loadEvents() {
        val filter = currentFilter.value
        loadWithState(viewModelScope) {
            val count = withContext(ioDispatcher) { repository.getCount(filter) }
            emitState { copy(isLoading = false, eventCount = count) }
        }
    }

    /**
     * Returns all crash events as a JSONL string (one JSON object per line).
     * Returns null if no events exist or on error.
     */
    suspend fun exportEvents(): String? = withContext(ioDispatcher) {
        try {
            val events = repository.getAll(CrashFilter(), limit = Int.MAX_VALUE, offset = 0)
            if (events.isEmpty()) return@withContext null
            events.joinToString("\n") { it.toJsonLine() }
        } catch (e: Exception) {
            safeLog("CrashHistoryViewModel", "exportEvents failed", e)
            null
        }
    }

    /**
     * Computes crash statistics from all stored events.
     * Returns empty statistics on error.
     */
    suspend fun getStatistics(): CrashStatistics = withContext(ioDispatcher) {
        try {
            val events = repository.getAll(CrashFilter(), limit = Int.MAX_VALUE, offset = 0)
            CrashStatistics.from(events)
        } catch (e: Exception) {
            safeLog("CrashHistoryViewModel", "getStatistics failed", e)
            CrashStatistics(0, 0, 0L, emptyList())
        }
    }

    companion object {
        const val PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 50
    }
}

data class CrashStatistics(
    val totalCount: Int,
    val uniquePackageCount: Int,
    val mostRecentTimestampMs: Long,
    val topPackages: List<Pair<String, Int>>,
) {
    companion object {
        fun from(events: List<CrashEvent>): CrashStatistics {
            if (events.isEmpty()) {
                return CrashStatistics(0, 0, 0L, emptyList())
            }
            val byPackage = events.groupingBy { it.packageName }.eachCount()
            val topPackages = byPackage.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key to it.value }
            val mostRecent = events.maxOf { it.timestampMs }
            return CrashStatistics(
                totalCount = events.size,
                uniquePackageCount = byPackage.size,
                mostRecentTimestampMs = mostRecent,
                topPackages = topPackages,
            )
        }
    }
}
