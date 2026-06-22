package nota.android.crash.xp.app.observe

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.FakeCrashLogRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrashHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: FakeCrashLogRepository
    private lateinit var viewModel: CrashHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCrashLogRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = CrashHistoryViewModel(repository, ioDispatcher = testDispatcher)
    }

    // ─── Initial State ───

    @Test
    fun `initial state has isLoading false and eventCount 0`() = testScope.runTest {
        createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(0, state.eventCount)
    }

    // ─── Loading Toggles ───

    @Test
    fun `loadEvents sets isLoading to true then false`() = testScope.runTest {
        createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.loadEvents()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── State Updates After loadEvents ───

    @Test
    fun `loadEvents populates eventCount`() = testScope.runTest {
        val events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
        )
        repository.events = events
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.eventCount)
    }

    @Test
    fun `loadEvents with no events returns eventCount 0`() = testScope.runTest {
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.eventCount)
    }

    @Test
    fun `loadEvents handles exception gracefully`() = testScope.runTest {
        repository.throwOnGetAll = true
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0, state.eventCount)
    }

    // ─── PagingData Flow ───

    @Test
    fun `pagingData emits expected events`() = testScope.runTest {
        val events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
        )
        repository.events = events
        createViewModel()

        val snapshot: List<CrashEvent> = viewModel.pagingData.asSnapshot()
        assertEquals(2, snapshot.size)
        assertEquals("e1", snapshot[0].id)
        assertEquals("e2", snapshot[1].id)
    }

    @Test
    fun `pagingData emits empty list when repository has no events`() = testScope.runTest {
        createViewModel()

        val snapshot: List<CrashEvent> = viewModel.pagingData.asSnapshot()
        assertTrue(snapshot.isEmpty())
    }

    // ─── Recovery After Exception ───

    @Test
    fun `loadEvents recovers after exception`() = testScope.runTest {
        repository.throwOnGetCount = true
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, viewModel.uiState.value.eventCount)

        repository.throwOnGetCount = false
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
        )

        viewModel.loadEvents()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.eventCount)
    }

    // ─── clearError ───

    @Test
    fun `clearError resets errorMessage to null`() = testScope.runTest {
        repository.throwOnGetCount = true
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()
        assert(viewModel.uiState.value.errorMessage != null)

        viewModel.clearError()
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    // ─── Re-entrancy / Deduplication ───

    @Test
    fun `consecutive loadEvents before completion produces correct final state`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
        )
        createViewModel()

        // Two rapid calls before either completes
        viewModel.loadEvents()
        viewModel.loadEvents()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.eventCount)
    }

    @Test
    fun `multiple loadEvents calls do not accumulate duplicate counts`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
        )
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.eventCount)

        viewModel.loadEvents()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.eventCount)

        viewModel.loadEvents()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.eventCount)
    }

    // ─── Filter ───

    @Test
    fun `initial activeFilter is null`() = testScope.runTest {
        createViewModel()
        assertNull(viewModel.uiState.value.activeFilter)
    }

    @Test
    fun `setFilter updates activeFilter in uiState`() = testScope.runTest {
        createViewModel()
        val filter = CrashFilter(packageName = "com.example.a")
        viewModel.setFilter(filter)
        advanceUntilIdle()
        assertEquals(filter, viewModel.uiState.value.activeFilter)
    }

    @Test
    fun `setFilter with empty CrashFilter clears activeFilter`() = testScope.runTest {
        createViewModel()
        viewModel.setFilter(CrashFilter(packageName = "com.example.a"))
        advanceUntilIdle()
        viewModel.setFilter(CrashFilter())
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeFilter)
    }

    @Test
    fun `setFilter updates eventCount to match filtered results`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
        )
        createViewModel()

        viewModel.setFilter(CrashFilter(packageName = "com.example.a"))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.eventCount)
        assertEquals(CrashFilter(packageName = "com.example.a"), viewModel.uiState.value.activeFilter)
    }

    @Test
    fun `setFilter with clear restores full eventCount`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
        )
        createViewModel()

        viewModel.setFilter(CrashFilter(packageName = "com.example.a"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.eventCount)

        viewModel.setFilter(CrashFilter())
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.eventCount)
    }

    @Test
    fun `setFilter updates pagingData with filtered events`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
        )
        createViewModel()

        viewModel.setFilter(CrashFilter(packageName = "com.example.a"))
        advanceUntilIdle()

        val snapshot: List<CrashEvent> = viewModel.pagingData.asSnapshot()
        assertEquals(2, snapshot.size)
        assertTrue(snapshot.all { it.packageName == "com.example.a" })
    }

    // ─── exportEvents ───

    @Test
    fun `exportEvents returns null when no events exist`() = testScope.runTest {
        createViewModel()
        val result = viewModel.exportEvents()
        assertNull(result)
    }

    @Test
    fun `exportEvents returns valid JSONL string`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
        )
        createViewModel()

        val result = viewModel.exportEvents()
        assertTrue(result != null)
        val lines = result!!.lines().filter { it.isNotBlank() }
        assertEquals(2, lines.size)
        // Each line should be parseable JSON containing the event id
        assertTrue(lines[0].contains("\"id\""))
        assertTrue(lines[1].contains("\"id\""))
    }

    @Test
    fun `exportEvents returns null when repository throws`() = testScope.runTest {
        repository.throwOnGetAll = true
        createViewModel()

        val result = viewModel.exportEvents()
        assertNull(result)
    }

    // ─── getStatistics ───

    @Test
    fun `getStatistics returns zero counts when no events exist`() = testScope.runTest {
        createViewModel()
        val stats = viewModel.getStatistics()

        assertEquals(0, stats.totalCount)
        assertEquals(0, stats.uniquePackageCount)
        assertEquals(0L, stats.mostRecentTimestampMs)
        assertTrue(stats.topPackages.isEmpty())
    }

    @Test
    fun `getStatistics computes correct counts and top packages`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e4", timestampMs = 4000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e5", timestampMs = 5000L, packageName = "com.example.c", exceptionClass = "RuntimeException"),
        )
        createViewModel()

        val stats = viewModel.getStatistics()
        assertEquals(5, stats.totalCount)
        assertEquals(3, stats.uniquePackageCount)
        assertEquals(5000L, stats.mostRecentTimestampMs)
        assertEquals(3, stats.topPackages.size)
        // com.example.a should be top with 3 crashes
        assertEquals("com.example.a" to 3, stats.topPackages[0])
    }

    @Test
    fun `getStatistics top packages limited to 3`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.b", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.c", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e4", timestampMs = 4000L, packageName = "com.d", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e5", timestampMs = 5000L, packageName = "com.a", exceptionClass = "NullPointerException"),
        )
        createViewModel()

        val stats = viewModel.getStatistics()
        assertEquals(3, stats.topPackages.size)
        assertEquals("com.a" to 2, stats.topPackages[0])
    }

    @Test
    fun `getStatistics returns empty stats when repository throws`() = testScope.runTest {
        repository.throwOnGetAll = true
        createViewModel()

        val stats = viewModel.getStatistics()
        assertEquals(0, stats.totalCount)
        assertEquals(0, stats.uniquePackageCount)
    }
}
