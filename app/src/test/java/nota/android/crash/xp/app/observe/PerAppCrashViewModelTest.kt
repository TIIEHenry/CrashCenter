package nota.android.crash.xp.app.observe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerAppCrashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: FakeCrashLogRepository
    private lateinit var viewModel: PerAppCrashViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCrashLogRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(packageName: String = "com.example.app") {
        viewModel = PerAppCrashViewModel(
            packageName = packageName,
            repository = repository,
            ioDispatcher = testDispatcher,
        )
    }

    // ─── Initial State ───

    @Test
    fun `initial state has isLoading true and null summary`() = testScope.runTest {
        createViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertNull(state.summary)
    }

    // ─── loadSummary ───

    @Test
    fun `loadSummary sets isLoading to false after completion`() = testScope.runTest {
        createViewModel()
        viewModel.loadSummary()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadSummary populates summary with zero counts when no events`() = testScope.runTest {
        createViewModel()
        viewModel.loadSummary()
        advanceUntilIdle()

        val summary = viewModel.uiState.value.summary
        assertNotNull(summary)
        assertEquals(0, summary!!.totalCount)
        assertEquals(0L, summary.mostRecentTimestampMs)
        assertNull(summary.topExceptionClass)
        assertEquals(0, summary.topExceptionCount)
    }

    @Test
    fun `loadSummary computes correct stats for matching package`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.app", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 3000L, packageName = "com.example.app", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e3", timestampMs = 2000L, packageName = "com.example.app", exceptionClass = "IllegalStateException"),
            CrashEvent(id = "e4", timestampMs = 500L, packageName = "com.other.app", exceptionClass = "RuntimeException"),
        )
        createViewModel("com.example.app")
        viewModel.loadSummary()
        advanceUntilIdle()

        val summary = viewModel.uiState.value.summary!!
        assertEquals(3, summary.totalCount)
        assertEquals(3000L, summary.mostRecentTimestampMs)
        assertEquals("NullPointerException", summary.topExceptionClass)
        assertEquals(2, summary.topExceptionCount)
    }

    @Test
    fun `loadSummary filters events by packageName`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.app", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.other.app", exceptionClass = "RuntimeException"),
        )
        createViewModel("com.example.app")
        viewModel.loadSummary()
        advanceUntilIdle()

        val summary = viewModel.uiState.value.summary!!
        assertEquals(1, summary.totalCount)
    }

    // ─── Error Path ───

    @Test
    fun `loadSummary handles exception gracefully`() = testScope.runTest {
        repository.throwOnGetAll = true
        createViewModel()
        viewModel.loadSummary()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `clearError resets errorMessage to null`() = testScope.runTest {
        repository.throwOnGetAll = true
        createViewModel()
        viewModel.loadSummary()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── Paging Data ───

    @Test
    fun `pagingData is not null`() = testScope.runTest {
        createViewModel()
        assertNotNull(viewModel.pagingData)
    }
}
