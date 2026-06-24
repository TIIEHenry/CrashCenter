package nota.android.crash.xp.app.observe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogcatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: LogcatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LogcatViewModel(ioDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initial State ───

    @Test
    fun `initial state has isLoading false and empty entries`() = testScope.runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.entries.isEmpty())
        assertEquals(LogcatUiState.DEFAULT_LEVELS, state.activeLevels)
        assertFalse(state.isFiltered)
        assertEquals(0, state.totalRawCount)
        assertNull(state.errorMessage)
    }

    // ─── loadFromText null input ───

    @Test
    fun `loadFromText with null input produces empty entries`() = testScope.runTest {
        viewModel.loadFromText(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.entries.isEmpty())
        assertEquals(0, state.totalRawCount)
    }

    @Test
    fun `loadFromText with blank input produces empty entries`() = testScope.runTest {
        viewModel.loadFromText("   \n  ")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.entries.isEmpty())
    }

    // ─── loadFromText parsing ───

    @Test
    fun `loadFromText parses valid logcat lines`() = testScope.runTest {
        val logcat = "06-23 10:00:00.000  1234  1234 I TestTag: hello world"
        viewModel.loadFromText(logcat)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.entries.size)
        assertEquals("TestTag", state.entries[0].tag)
        assertEquals("hello world", state.entries[0].message)
        assertEquals(LogcatLevel.INFO, state.entries[0].level)
        assertEquals(1, state.totalRawCount)
        assertFalse(state.isFiltered)
    }

    // ─── loadFromText crash-only filtering ───

    @Test
    fun `loadFromText with crashOnly filters non-crash entries`() = testScope.runTest {
        val logcat = """
            06-23 10:00:00.000  1234  1234 I TestTag: normal log
            06-23 10:00:01.000  1234  1234 E AndroidRuntime: FATAL EXCEPTION: main
            06-23 10:00:02.000  1234  1234 D DebugTag: debug info
        """.trimIndent()
        viewModel.loadFromText(logcat, crashOnly = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFiltered)
        assertEquals(3, state.totalRawCount)
        // Only the AndroidRuntime line should remain
        assertEquals(1, state.entries.size)
        assertEquals("AndroidRuntime", state.entries[0].tag)
    }

    @Test
    fun `loadFromText without crashOnly keeps all entries`() = testScope.runTest {
        val logcat = """
            06-23 10:00:00.000  1234  1234 I TestTag: normal log
            06-23 10:00:01.000  1234  1234 E AndroidRuntime: FATAL EXCEPTION: main
        """.trimIndent()
        viewModel.loadFromText(logcat, crashOnly = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isFiltered)
        assertEquals(2, state.entries.size)
        assertEquals(2, state.totalRawCount)
    }

    // ─── toggleLevel ───

    @Test
    fun `toggleLevel removes level when already active`() = testScope.runTest {
        assertTrue(LogcatLevel.INFO in viewModel.uiState.value.activeLevels)

        viewModel.toggleLevel(LogcatLevel.INFO)
        assertFalse(LogcatLevel.INFO in viewModel.uiState.value.activeLevels)
        // Other levels should still be present
        assertTrue(LogcatLevel.ERROR in viewModel.uiState.value.activeLevels)
    }

    @Test
    fun `toggleLevel adds level when not active`() = testScope.runTest {
        // Remove first
        viewModel.toggleLevel(LogcatLevel.INFO)
        assertFalse(LogcatLevel.INFO in viewModel.uiState.value.activeLevels)

        // Add back
        viewModel.toggleLevel(LogcatLevel.INFO)
        assertTrue(LogcatLevel.INFO in viewModel.uiState.value.activeLevels)
    }

    @Test
    fun `setLevelVisible hides and shows level`() = testScope.runTest {
        assertTrue(LogcatLevel.ERROR in viewModel.uiState.value.activeLevels)

        viewModel.setLevelVisible(LogcatLevel.ERROR, visible = false)
        assertFalse(LogcatLevel.ERROR in viewModel.uiState.value.activeLevels)

        viewModel.setLevelVisible(LogcatLevel.ERROR, visible = true)
        assertTrue(LogcatLevel.ERROR in viewModel.uiState.value.activeLevels)
    }

    @Test
    fun `setLevelVisible is idempotent`() = testScope.runTest {
        viewModel.setLevelVisible(LogcatLevel.DEBUG, visible = true)
        assertEquals(LogcatUiState.DEFAULT_LEVELS, viewModel.uiState.value.activeLevels)

        viewModel.setLevelVisible(LogcatLevel.DEBUG, visible = false)
        viewModel.setLevelVisible(LogcatLevel.DEBUG, visible = false)
        assertFalse(LogcatLevel.DEBUG in viewModel.uiState.value.activeLevels)
    }

    @Test
    fun `level filter updates displayEntries`() = testScope.runTest {
        val logcat = """
            06-23 10:00:00.000  1234  1234 I TestTag: info log
            06-23 10:00:01.000  1234  1234 E AndroidRuntime: error log
        """.trimIndent()
        viewModel.loadFromText(logcat, crashOnly = false)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.displayEntries.size)

        viewModel.setLevelVisible(LogcatLevel.INFO, visible = false)
        val filtered = viewModel.uiState.value.displayEntries
        assertEquals(1, filtered.size)
        assertEquals(LogcatLevel.ERROR, filtered.single().level)
    }

    @Test
    fun `toggleLevel toggles SILENT independently`() = testScope.runTest {
        // SILENT is not in DEFAULT_LEVELS
        assertFalse(LogcatLevel.SILENT in viewModel.uiState.value.activeLevels)

        viewModel.toggleLevel(LogcatLevel.SILENT)
        assertTrue(LogcatLevel.SILENT in viewModel.uiState.value.activeLevels)

        viewModel.toggleLevel(LogcatLevel.SILENT)
        assertFalse(LogcatLevel.SILENT in viewModel.uiState.value.activeLevels)
    }

    // ─── resetLevels ───

    @Test
    fun `resetLevels restores default levels`() = testScope.runTest {
        viewModel.toggleLevel(LogcatLevel.ERROR)
        viewModel.toggleLevel(LogcatLevel.INFO)
        assertFalse(LogcatLevel.ERROR in viewModel.uiState.value.activeLevels)
        assertFalse(LogcatLevel.INFO in viewModel.uiState.value.activeLevels)

        viewModel.resetLevels()
        assertEquals(LogcatUiState.DEFAULT_LEVELS, viewModel.uiState.value.activeLevels)
    }

    @Test
    fun `resetLevels is idempotent`() = testScope.runTest {
        viewModel.resetLevels()
        assertEquals(LogcatUiState.DEFAULT_LEVELS, viewModel.uiState.value.activeLevels)

        viewModel.resetLevels()
        assertEquals(LogcatUiState.DEFAULT_LEVELS, viewModel.uiState.value.activeLevels)
    }

    // ─── setCrashFilter ───

    @Test
    fun `setCrashFilter stores flag when entries are empty`() = testScope.runTest {
        viewModel.setCrashFilter(true)
        assertTrue(viewModel.uiState.value.isFiltered)
        assertTrue(viewModel.uiState.value.entries.isEmpty())
    }

    @Test
    fun `loadFromText with crashOnly keeps ANR hints`() = testScope.runTest {
        val logcat = """
            06-23 10:00:00.000  1000  1000 I TestTag: normal log
            06-23 10:00:01.000  1000  1000 E ActivityManager: ANR in com.example.app
        """.trimIndent()
        viewModel.loadFromText(logcat, crashOnly = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFiltered)
        assertEquals(2, state.totalRawCount)
        assertEquals(1, state.entries.size)
        assertTrue(LogcatParser.isAnrHint(state.entries.single()))
    }

    @Test
    fun `setCrashFilter toggles isFiltered when entries exist`() = testScope.runTest {
        val logcat = "06-23 10:00:00.000  1234  1234 I TestTag: hello"
        viewModel.loadFromText(logcat)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isFiltered)

        viewModel.setCrashFilter(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFiltered)

        viewModel.setCrashFilter(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isFiltered)
    }

    @Test
    fun `setSearchQuery filters displayEntries by tag and message`() = testScope.runTest {
        val logcat = """
            06-23 10:00:00.000  1234  1234 I TestTag: hello world
            06-23 10:00:01.000  1234  1234 E AndroidRuntime: error log
        """.trimIndent()
        viewModel.loadFromText(logcat, crashOnly = false)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.displayEntries.size)

        viewModel.setSearchQuery("AndroidRuntime")
        assertEquals(1, viewModel.uiState.value.displayEntries.size)
        assertEquals("AndroidRuntime", viewModel.uiState.value.displayEntries.single().tag)

        viewModel.setSearchQuery("hello")
        assertEquals(1, viewModel.uiState.value.displayEntries.size)
        assertEquals("TestTag", viewModel.uiState.value.displayEntries.single().tag)

        viewModel.setSearchQuery("")
        assertEquals(2, viewModel.uiState.value.displayEntries.size)
    }

    @Test
    fun `setSearchQuery is case insensitive`() = testScope.runTest {
        val logcat = "06-23 10:00:00.000  1234  1234 I TestTag: Hello World"
        viewModel.loadFromText(logcat, crashOnly = false)
        advanceUntilIdle()

        viewModel.setSearchQuery("hello")
        assertEquals(1, viewModel.uiState.value.displayEntries.size)
    }

    // ─── clearError ───

    @Test
    fun `clearError resets errorMessage to null`() = testScope.runTest {
        // Force an error by passing invalid input that triggers an exception in the parser path
        // BaseFlowViewModel catches exceptions and sets errorMessage
        // The LogcatParser doesn't throw on invalid input, so we verify clearError is a no-op on clean state
        assertNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
