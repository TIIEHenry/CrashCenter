package nota.android.crash.xp.app.shell

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ShellViewModel {
        return ShellViewModel(savedStateHandle)
    }

    // ─── Initial State ───

    @Test
    fun `initial state defaults to CONFIG tab and xposedActive false`() = testScope.runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(ShellTab.CONFIG, state.selectedTab)
        assertFalse(state.xposedActive)
    }

    @Test
    fun `initial state restores selected tab from SavedStateHandle`() = testScope.runTest {
        savedStateHandle["selected_tab"] = ShellTab.OBSERVE.name
        val viewModel = createViewModel()

        assertEquals(ShellTab.OBSERVE, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `initial state handles invalid tab name in SavedStateHandle`() = testScope.runTest {
        savedStateHandle["selected_tab"] = "INVALID_TAB"
        val viewModel = createViewModel()

        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)
    }

    // ─── State Updates ───

    @Test
    fun `setSelectedTab updates selected tab`() = testScope.runTest {
        val viewModel = createViewModel()
        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)

        viewModel.setSelectedTab(ShellTab.OBSERVE)

        assertEquals(ShellTab.OBSERVE, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `setSelectedTab persists tab to SavedStateHandle`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.setSelectedTab(ShellTab.OBSERVE)

        assertEquals(ShellTab.OBSERVE.name, savedStateHandle.get<String>("selected_tab"))
    }

    @Test
    fun `refreshXposedStatus updates xposedActive`() = testScope.runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.xposedActive)

        viewModel.refreshXposedStatus(true)
        assertTrue(viewModel.uiState.value.xposedActive)

        viewModel.refreshXposedStatus(false)
        assertFalse(viewModel.uiState.value.xposedActive)
    }

    @Test
    fun `multiple tab switches reflect latest state`() = testScope.runTest {
        val viewModel = createViewModel()

        viewModel.setSelectedTab(ShellTab.OBSERVE)
        assertEquals(ShellTab.OBSERVE, viewModel.uiState.value.selectedTab)

        viewModel.setSelectedTab(ShellTab.CONFIG)
        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)

        assertEquals(ShellTab.CONFIG.name, savedStateHandle.get<String>("selected_tab"))
    }

    @Test
    fun `xposed status changes are reflected in state`() = testScope.runTest {
        val viewModel = createViewModel()

        viewModel.refreshXposedStatus(true)
        assertTrue(viewModel.uiState.value.xposedActive)

        viewModel.refreshXposedStatus(false)
        assertFalse(viewModel.uiState.value.xposedActive)

        viewModel.refreshXposedStatus(true)
        assertTrue(viewModel.uiState.value.xposedActive)
    }
}
