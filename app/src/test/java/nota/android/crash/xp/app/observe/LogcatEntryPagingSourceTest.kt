package nota.android.crash.xp.app.observe

import androidx.paging.PagingSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogcatEntryPagingSourceTest {

    private val entries = (1..120).map { index ->
        LogcatEntry(
            timestamp = "06-23 10:00:00.000",
            pid = "1000",
            tid = "1000",
            level = LogcatLevel.INFO,
            tag = "Test",
            message = "line $index",
        )
    }

    @Test
    fun `first page returns requested size`() = runTest {
        val source = LogcatEntryPagingSource(entries)
        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 50,
                placeholdersEnabled = false,
            ),
        ) as PagingSource.LoadResult.Page

        assertEquals(50, result.data.size)
        assertNull(result.prevKey)
        assertEquals(1, result.nextKey)
    }

    @Test
    fun `last page has no next key`() = runTest {
        val source = LogcatEntryPagingSource(entries)
        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = 2,
                loadSize = 50,
                placeholdersEnabled = false,
            ),
        ) as PagingSource.LoadResult.Page

        assertEquals(20, result.data.size)
        assertEquals(1, result.prevKey)
        assertNull(result.nextKey)
    }
}
