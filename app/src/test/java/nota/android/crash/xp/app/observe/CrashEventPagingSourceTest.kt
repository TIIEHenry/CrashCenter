package nota.android.crash.xp.app.observe

import androidx.paging.PagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.FakeCrashLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrashEventPagingSourceTest {

    @Test
    fun `load returns page of events`() = runTest {
        val repository = FakeCrashLogRepository()
        val events = List(100) { idx ->
            CrashEvent(
                id = "e$idx",
                timestampMs = idx.toLong(),
                packageName = "com.example.$idx",
                exceptionClass = "NullPointerException",
            )
        }
        repository.events = events

        val pagingSource = CrashEventPagingSource(repository, CrashFilter())
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 0, loadSize = 20, placeholdersEnabled = false)
        )

        val page = result as PagingSource.LoadResult.Page
        assertEquals(20, page.data.size)
        assertEquals("e0", page.data[0].id)
        assertEquals("e19", page.data[19].id)
        assertNull(page.prevKey)
        assertEquals(1, page.nextKey)
    }

    @Test
    fun `load returns null nextKey when fewer items than pageSize`() = runTest {
        val repository = FakeCrashLogRepository()
        repository.events = List(15) { idx ->
            CrashEvent(
                id = "e$idx",
                timestampMs = idx.toLong(),
                packageName = "com.example.$idx",
                exceptionClass = "NullPointerException",
            )
        }

        val pagingSource = CrashEventPagingSource(repository, CrashFilter())
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 0, loadSize = 20, placeholdersEnabled = false)
        )

        val page = result as PagingSource.LoadResult.Page
        assertEquals(15, page.data.size)
        assertNull(page.nextKey)
    }

    @Test
    fun `load returns error on exception`() = runTest {
        val repository = FakeCrashLogRepository()
        repository.throwOnGetAll = true

        val pagingSource = CrashEventPagingSource(repository, CrashFilter())
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = 0, loadSize = 20, placeholdersEnabled = false)
        )

        assertTrue(result is PagingSource.LoadResult.Error)
    }

    @Test
    fun `load with page key returns correct offset`() = runTest {
        val repository = FakeCrashLogRepository()
        repository.events = List(100) { idx ->
            CrashEvent(
                id = "e$idx",
                timestampMs = idx.toLong(),
                packageName = "com.example.$idx",
                exceptionClass = "NullPointerException",
            )
        }

        val pagingSource = CrashEventPagingSource(repository, CrashFilter())
        val result = pagingSource.load(
            PagingSource.LoadParams.Append(key = 2, loadSize = 20, placeholdersEnabled = false)
        )

        val page = result as PagingSource.LoadResult.Page
        assertEquals(20, page.data.size)
        assertEquals("e40", page.data[0].id)
        assertEquals("e59", page.data[19].id)
        assertEquals(1, page.prevKey)
        assertEquals(3, page.nextKey)
    }
}
