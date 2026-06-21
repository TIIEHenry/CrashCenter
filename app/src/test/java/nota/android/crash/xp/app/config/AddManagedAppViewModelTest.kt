package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AddManagedAppViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: ManagedAppRepository
    private lateinit var viewModel: AddManagedAppViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = AddManagedAppViewModel(repository)
    }

    // ─── Initial State ───

    @Test
    fun `initial state is Loading`() = testScope.runTest {
        whenever(repository.loadPickableApps()).thenReturn(emptyList())
        createViewModel()
        val state = viewModel.uiState.value
        assertTrue(state is AddManagedAppUiState.Loading)
    }

    @Test
    fun `init loads pickable apps and emits Success`() = testScope.runTest {
        val apps = listOf(
            fakePickableApp("com.example.a", "App A"),
            fakePickableApp("com.example.b", "App B"),
        )
        whenever(repository.loadPickableApps()).thenReturn(apps)
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AddManagedAppUiState.Success)
        val success = state as AddManagedAppUiState.Success
        assertEquals(2, success.apps.size)
        assertEquals("App A", success.apps[0].label)
        assertEquals("App B", success.apps[1].label)
        assertEquals("", success.query)
    }

    @Test
    fun `init with empty pickable apps emits Success with empty list`() = testScope.runTest {
        whenever(repository.loadPickableApps()).thenReturn(emptyList())
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AddManagedAppUiState.Success)
        val success = state as AddManagedAppUiState.Success
        assertTrue(success.apps.isEmpty())
    }

    @Test
    fun `init handles repository exception gracefully`() = testScope.runTest {
        whenever(repository.loadPickableApps()).thenThrow(RuntimeException("Simulated error"))
        viewModel = AddManagedAppViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AddManagedAppUiState.Success)
        val success = state as AddManagedAppUiState.Success
        assertTrue(success.apps.isEmpty())
    }

    // ─── Query Filtering ───

    @Test
    fun `setQuery filters apps by label`() = testScope.runTest {
        val apps = listOf(
            fakePickableApp("com.example.alpha", "Alpha Browser"),
            fakePickableApp("com.example.beta", "Beta Mail"),
        )
        whenever(repository.loadPickableApps()).thenReturn(apps)
        createViewModel()
        advanceUntilIdle()

        viewModel.setQuery("Alpha")
        advanceUntilIdle()

        val state = viewModel.uiState.value as AddManagedAppUiState.Success
        assertEquals(1, state.apps.size)
        assertEquals("Alpha Browser", state.apps[0].label)
        assertEquals("Alpha", state.query)
    }

    @Test
    fun `setQuery filters apps by package name`() = testScope.runTest {
        val apps = listOf(
            fakePickableApp("com.example.alpha", "Alpha"),
            fakePickableApp("com.example.beta", "Beta"),
        )
        whenever(repository.loadPickableApps()).thenReturn(apps)
        createViewModel()
        advanceUntilIdle()

        viewModel.setQuery("beta")
        advanceUntilIdle()

        val state = viewModel.uiState.value as AddManagedAppUiState.Success
        assertEquals(1, state.apps.size)
        assertEquals("Beta", state.apps[0].label)
    }

    @Test
    fun `setQuery with empty string shows all apps`() = testScope.runTest {
        val apps = listOf(
            fakePickableApp("com.example.alpha", "Alpha Browser"),
            fakePickableApp("com.example.beta", "Beta Mail"),
        )
        whenever(repository.loadPickableApps()).thenReturn(apps)
        createViewModel()
        advanceUntilIdle()

        viewModel.setQuery("Alpha")
        advanceUntilIdle()
        assertEquals(1, (viewModel.uiState.value as AddManagedAppUiState.Success).apps.size)

        viewModel.setQuery("")
        advanceUntilIdle()

        val state = viewModel.uiState.value as AddManagedAppUiState.Success
        assertEquals(2, state.apps.size)
        assertEquals("", state.query)
    }

    @Test
    fun `setQuery with no matches returns empty list`() = testScope.runTest {
        val apps = listOf(
            fakePickableApp("com.example.a", "App A"),
            fakePickableApp("com.example.b", "App B"),
        )
        whenever(repository.loadPickableApps()).thenReturn(apps)
        createViewModel()
        advanceUntilIdle()

        viewModel.setQuery("zzz")
        advanceUntilIdle()

        val state = viewModel.uiState.value as AddManagedAppUiState.Success
        assertTrue(state.apps.isEmpty())
        assertEquals("zzz", state.query)
    }

    @Test
    fun `setQuery is case insensitive`() = testScope.runTest {
        val apps = listOf(
            fakePickableApp("com.example.alpha", "Alpha"),
            fakePickableApp("com.example.beta", "Beta"),
        )
        whenever(repository.loadPickableApps()).thenReturn(apps)
        createViewModel()
        advanceUntilIdle()

        viewModel.setQuery("ALPHA")
        advanceUntilIdle()

        val state = viewModel.uiState.value as AddManagedAppUiState.Success
        assertEquals(1, state.apps.size)
        assertEquals("Alpha", state.apps[0].label)
    }

    // ─── Helpers ───

    private fun fakePickableApp(
        packageName: String,
        label: String,
        isSystem: Boolean = false,
    ): PickableApp = PickableApp(
        packageName = packageName,
        label = label,
        appInfo = ApplicationInfo().apply {
            this.packageName = packageName
            flags = if (isSystem) ApplicationInfo.FLAG_SYSTEM else 0
        },
        isSystem = isSystem,
    )
}
