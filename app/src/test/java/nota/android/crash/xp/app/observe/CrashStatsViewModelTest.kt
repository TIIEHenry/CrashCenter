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
class CrashStatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: FakeCrashLogRepository
    private lateinit var viewModel: CrashStatsViewModel

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
        viewModel = CrashStatsViewModel(repository, ioDispatcher = testDispatcher)
    }

    // ─── Initial State ───

    @Test
    fun `initial state has isLoading true and null stats`() = testScope.runTest {
        createViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertNull(state.stats)
    }

    // ─── loadStats ───

    @Test
    fun `loadStats sets isLoading to false after completion`() = testScope.runTest {
        createViewModel()
        viewModel.loadStats()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadStats populates stats with zero counts when no events`() = testScope.runTest {
        createViewModel()
        viewModel.loadStats()
        advanceUntilIdle()

        val stats = viewModel.uiState.value.stats
        assertNotNull(stats)
        assertEquals(0, stats!!.totalCount)
        assertEquals(0, stats.uniquePackageCount)
        assertEquals(0L, stats.mostRecentTimestampMs)
        assertTrue(stats.topPackages.isEmpty())
        assertTrue(stats.topExceptionClasses.isEmpty())
        assertTrue(stats.topCategories.isEmpty())
        assertTrue(stats.topClusters.isEmpty())
        assertTrue(stats.dailyCounts.isEmpty())
    }

    @Test
    fun `loadStats computes correct stats for multiple events`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.b", exceptionClass = "IllegalStateException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e4", timestampMs = 4000L, packageName = "com.a", exceptionClass = "RuntimeException"),
            CrashEvent(id = "e5", timestampMs = 5000L, packageName = "com.c", exceptionClass = "NullPointerException"),
        )
        createViewModel()
        viewModel.loadStats()
        advanceUntilIdle()

        val stats = viewModel.uiState.value.stats!!
        assertEquals(5, stats.totalCount)
        assertEquals(3, stats.uniquePackageCount)
        assertEquals(5000L, stats.mostRecentTimestampMs)

        // Top packages: com.a=3, com.b=1, com.c=1
        assertEquals(3, stats.topPackages.size)
        assertEquals("com.a", stats.topPackages[0].packageName)
        assertEquals(3, stats.topPackages[0].count)

        // Top exceptions: NullPointerException=3, others=1 each
        assertEquals("NullPointerException", stats.topExceptionClasses[0].exceptionClass)
        assertEquals(3, stats.topExceptionClasses[0].count)
    }

    @Test
    fun `loadStats handles exception gracefully`() = testScope.runTest {
        repository.throwOnGetAll = true
        createViewModel()
        viewModel.loadStats()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }

    // ─── clearError ───

    @Test
    fun `clearError resets errorMessage to null`() = testScope.runTest {
        repository.throwOnGetAll = true
        createViewModel()
        viewModel.loadStats()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── Re-entrancy ───

    @Test
    fun `consecutive loadStats calls produce correct final state`() = testScope.runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.a", exceptionClass = "NullPointerException"),
        )
        createViewModel()

        viewModel.loadStats()
        viewModel.loadStats()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.stats!!.totalCount)
    }
}
