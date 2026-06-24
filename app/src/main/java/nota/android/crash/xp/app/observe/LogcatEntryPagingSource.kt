package nota.android.crash.xp.app.observe

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * In-memory paging over a pre-parsed logcat list.
 * Matches the crash-history list pipeline so fast scroll behaves consistently.
 */
class LogcatEntryPagingSource(
    private val entries: List<LogcatEntry>,
) : PagingSource<Int, LogcatEntry>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LogcatEntry> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val fromIndex = page * pageSize
        if (fromIndex >= entries.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null,
            )
        }
        val toIndex = minOf(fromIndex + pageSize, entries.size)
        val slice = entries.subList(fromIndex, toIndex)
        val prevKey = if (page == 0) null else page - 1
        val nextKey = if (toIndex >= entries.size) null else page + 1
        return LoadResult.Page(
            data = slice,
            prevKey = prevKey,
            nextKey = nextKey,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, LogcatEntry>): Int? {
        return state.anchorPosition?.let { anchor ->
            val anchorPage = state.closestPageToPosition(anchor)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
