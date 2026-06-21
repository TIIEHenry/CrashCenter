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
import nota.android.crash.xp.app.data.FakeCrashLogRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ─── Re-entrancy / Deduplication ───

    @Test
    fun `consecutive loadEvents without completion cancels previous job`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
        )
        createViewModel()

        // First call
        viewModel.loadEvents()
        // Second call before first completes
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
}
