package nota.android.crash.xp.app.observe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nota.android.crash.xp.app.data.CrashEvent
import nota.android.crash.xp.app.data.FakeCrashLogRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CrashDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeCrashLogRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCrashLogRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(crashId: String): CrashDetailViewModel {
        return CrashDetailViewModel(
            crashId = crashId,
            repository = repository,
            ioDispatcher = testDispatcher,
        )
    }

    // ─── Initial State ───

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        val viewModel = createViewModel("crash-1")
        val state = viewModel.uiState.value
        assertTrue(state is CrashDetailUiState.Loading)
    }

    // ─── Loading Crash Detail ───

    @Test
    fun `loadCrashDetail with event in repository uses shortExceptionClass as title`() = runTest(testDispatcher) {
        val event = CrashEvent(
            id = "crash-1",
            timestampMs = 1000L,
            packageName = "com.example.app",
            exceptionClass = "java.lang.NullPointerException",
            stackTrace = "java.lang.NullPointerException: oops\n    at com.example.Foo.bar(Foo.java:42)",
        )
        repository.addEvent(event)

        val viewModel = createViewModel("crash-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CrashDetailUiState.Success)
        val success = state as CrashDetailUiState.Success
        assertEquals("NullPointerException", success.title)
        assertTrue(success.stackTrace.contains("NullPointerException"))
    }

    @Test
    fun `loadCrashDetail with event only in repository uses stack trace from event`() = runTest(testDispatcher) {
        val event = CrashEvent(
            id = "crash-2",
            timestampMs = 2000L,
            packageName = "com.example.app",
            exceptionClass = "java.lang.IllegalStateException",
            stackTrace = "java.lang.IllegalStateException: Bad state\n    at com.example.Foo.bar(Foo.java:42)",
        )
        repository.addEvent(event)

        val viewModel = createViewModel("crash-2")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CrashDetailUiState.Success)
        val success = state as CrashDetailUiState.Success
        assertEquals("IllegalStateException", success.title)
        assertTrue(success.stackTrace.contains("IllegalStateException: Bad state"))
    }

    @Test
    fun `loadCrashDetail with unknown crashId shows not found message`() = runTest(testDispatcher) {
        val viewModel = createViewModel("nonexistent-id")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CrashDetailUiState.Success)
        val success = state as CrashDetailUiState.Success
        assertEquals("Crash detail not found", success.title)
        assertEquals("Crash detail not found: nonexistent-id", success.stackTrace)
    }

    @Test
    fun `loadCrashDetail with nested exception class extracts simple name`() = runTest(testDispatcher) {
        val event = CrashEvent(
            id = "crash-5",
            timestampMs = 5000L,
            packageName = "com.example.app",
            exceptionClass = "android.database.sqlite.SQLiteException",
            stackTrace = "android.database.sqlite.SQLiteException: no such table\n    at com.example.Db.query(Db.java:10)",
        )
        repository.addEvent(event)

        val viewModel = createViewModel("crash-5")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CrashDetailUiState.Success)
        val success = state as CrashDetailUiState.Success
        assertEquals("SQLiteException", success.title)
    }

    @Test
    fun `loadCrashDetail with message in exceptionClass includes message in short name`() = runTest(testDispatcher) {
        val event = CrashEvent(
            id = "crash-6",
            timestampMs = 6000L,
            packageName = "com.example.app",
            exceptionClass = "java.lang.IllegalArgumentException: Invalid parameter",
            stackTrace = "java.lang.IllegalArgumentException: Invalid parameter\n    at com.example.Test.validate(Test.java:20)",
        )
        repository.addEvent(event)

        val viewModel = createViewModel("crash-6")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CrashDetailUiState.Success)
        val success = state as CrashDetailUiState.Success
        assertEquals("IllegalArgumentException: Invalid parameter", success.title)
    }

    @Test
    fun `loadCrashDetail transitions from Loading to Success`() = runTest(testDispatcher) {
        val event = CrashEvent(
            id = "crash-7",
            timestampMs = 7000L,
            packageName = "com.example.app",
            exceptionClass = "java.lang.Exception",
            stackTrace = "Trace",
        )
        repository.addEvent(event)

        val viewModel = createViewModel("crash-7")

        // Initial state
        assertTrue(viewModel.uiState.value is CrashDetailUiState.Loading)

        advanceUntilIdle()

        // After loading
        assertTrue(viewModel.uiState.value is CrashDetailUiState.Success)
    }

    @Test
    fun `loadCrashDetail with empty stack trace uses fallback from event`() = runTest(testDispatcher) {
        val event = CrashEvent(
            id = "crash-4",
            timestampMs = 4000L,
            packageName = "com.example.app",
            exceptionClass = "java.lang.RuntimeException",
            stackTrace = "",
        )
        repository.addEvent(event)

        val viewModel = createViewModel("crash-4")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CrashDetailUiState.Success)
        val success = state as CrashDetailUiState.Success
        assertEquals("RuntimeException", success.title)
        assertTrue(success.stackTrace.contains("java.lang.RuntimeException"))
        assertTrue(success.stackTrace.contains("package=com.example.app"))
    }

    @Test
    fun `loadCrashDetail with Error class extracts simple name from stack trace`() = runTest(testDispatcher) {
        val event = CrashEvent(
            id = "crash-8",
            timestampMs = 8000L,
            packageName = "com.example.app",
            exceptionClass = "java.lang.Error",
            stackTrace = "java.lang.Error: Fatal\n    at com.example.Main.main(Main.java:1)",
        )
        repository.addEvent(event)

        val viewModel = createViewModel("crash-8")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CrashDetailUiState.Success)
        val success = state as CrashDetailUiState.Success
        assertEquals("Error", success.title)
    }

    @Test
    fun `loadCrashDetail with blank exceptionClass uses empty title`() = runTest(testDispatcher) {
        val event = CrashEvent(
            id = "crash-3",
            timestampMs = 3000L,
            packageName = "com.example.app",
            exceptionClass = "",
            stackTrace = "java.lang.RuntimeException: Something went wrong\n    at com.example.Test.run(Test.java:10)",
        )
        repository.addEvent(event)

        val viewModel = createViewModel("crash-3")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CrashDetailUiState.Success)
        val success = state as CrashDetailUiState.Success
        assertEquals("", success.title)
    }
}
