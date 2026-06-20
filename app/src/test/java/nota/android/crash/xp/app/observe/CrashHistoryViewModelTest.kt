package nota.android.crash.xp.app.observe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nota.android.crash.xp.app.data.CrashEvent
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
        viewModel = CrashHistoryViewModel(repository, testDispatcher)
    }

    // ─── Initial State ───

    @Test
    fun `initial state has empty events and isLoading false`() = testScope.runTest {
        createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.events.isEmpty())
        assertEquals(0, state.eventCount)
    }

    // ─── Loading Toggles ───

    @Test
    fun `loadEvents sets isLoading to true then false`() = testScope.runTest {
        createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.loadEvents()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── State Updates After loadEvents ───

    @Test
    fun `loadEvents populates events and eventCount`() = testScope.runTest {
        val events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
        )
        repository.events = events
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.events.size)
        assertEquals(2, state.eventCount)
        assertEquals("e1", state.events[0].id)
        assertEquals("e2", state.events[1].id)
    }

    @Test
    fun `loadEvents with no events returns empty list`() = testScope.runTest {
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.events.isEmpty())
        assertEquals(0, state.eventCount)
    }

    @Test
    fun `loadEvents does not reload if already loaded and forceReload is false`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
        )
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.events.size)

        // Add new event after first load
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
        )

        // Without forceReload, should not re-fetch
        viewModel.loadEvents()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.events.size)
    }

    @Test
    fun `loadEvents with forceReload fetches new data`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
        )
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.events.size)

        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
        )

        viewModel.loadEvents(forceReload = true)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.events.size)
    }

    // ─── Generation / Cancellation Logic ───

    @Test
    fun `consecutive loadEvents cancels previous generation`() = testScope.runTest {
        val events1 = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
        )
        val events2 = listOf(
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.b", exceptionClass = "IllegalStateException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.example.c", exceptionClass = "RuntimeException"),
        )
        repository.events = events1
        createViewModel()

        // Start first load
        viewModel.loadEvents()
        assertTrue(viewModel.uiState.value.isLoading)

        // Change repository and start second load before first completes
        repository.events = events2
        viewModel.loadEvents(forceReload = true)

        advanceUntilIdle()

        // Second load should win; first load's result is discarded
        val state = viewModel.uiState.value
        assertEquals(2, state.events.size)
        assertEquals("e2", state.events[0].id)
        assertEquals("e3", state.events[1].id)
    }

    @Test
    fun `loadEvents handles exception gracefully`() = testScope.runTest {
        repository.throwOnGetAll = true
        createViewModel()

        viewModel.loadEvents()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.events.isEmpty())
        assertEquals(0, state.eventCount)
    }
}
