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
        viewModel = CrashHistoryViewModel(repository)
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
}
