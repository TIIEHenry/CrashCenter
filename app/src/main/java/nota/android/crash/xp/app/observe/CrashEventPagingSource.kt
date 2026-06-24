package nota.android.crash.xp.app.observe

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.CrashLogRepository

class CrashEventPagingSource(
    private val repository: CrashLogRepository,
    private val filter: CrashFilter,
) : PagingSource<Int, CrashEvent>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CrashEvent> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            withContext(Dispatchers.IO) {
                val events = repository.getAll(filter, pageSize, offset)
                val prevKey = if (page > 0) page - 1 else null
                val nextKey = if (events.size == pageSize) page + 1 else null
                LoadResult.Page(
                    data = events,
                    prevKey = prevKey,
                    nextKey = nextKey,
                )
            }
        } catch (e: Exception) {
            try { Log.w("CrashEventPagingSource", "Paging load failed", e) } catch (_: Throwable) {}
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, CrashEvent>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
