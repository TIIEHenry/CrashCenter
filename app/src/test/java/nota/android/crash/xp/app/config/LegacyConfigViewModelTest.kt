package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nota.android.crash.xp.app.PackageVisibilityHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LegacyConfigViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyAppRepository
    private lateinit var visibilityRepository: PackageVisibilityRepository
    private lateinit var viewModel: LegacyConfigViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        visibilityRepository = mock()

        whenever(visibilityRepository.detectPackageVisibility()).thenReturn(
            PackageVisibilityHelper.Status(
                hasFullVisibility = true,
                permissionGranted = true,
                needsUserAction = false,
            )
        )
        whenever(visibilityRepository.detectPackageVisibilityAfterLoad(org.mockito.kotlin.any())).thenReturn(
            PackageVisibilityHelper.Status(
                hasFullVisibility = true,
                permissionGranted = true,
                needsUserAction = false,
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = LegacyConfigViewModel(repository, visibilityRepository, CoroutineScope(testDispatcher))
    }

    // ─── Initial State ───

    @Test
    fun `initial state reflects repository values`() = runTest {
        whenever(repository.readScopeMode()).thenReturn(true)
        whenever(repository.readHandleSystem()).thenReturn(true)
        whenever(repository.readShowSystemUi()).thenReturn(true)

        createViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.isLegacyMode)
        assertTrue(state.scopeMode)
        assertTrue(state.handleSystem)
        assertTrue(state.showSystemUi)
        assertFalse(state.isLoading)
        assertEquals("", state.query)
        assertEquals(HookFilter.ALL, state.hookFilter)
        assertTrue(state.allApps.isEmpty())
        assertTrue(state.visibleApps.isEmpty())
    }

    @Test
    fun `initial state with default repository values`() = runTest {
        createViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.scopeMode)
        assertFalse(state.handleSystem)
        assertFalse(state.showSystemUi)
    }

    @Test
    fun `initial state includes package visibility`() = runTest {
        val status = PackageVisibilityHelper.Status(
            hasFullVisibility = false,
            permissionGranted = false,
            needsUserAction = true,
        )
        whenever(visibilityRepository.detectPackageVisibility()).thenReturn(status)

        createViewModel()

        assertEquals(status, viewModel.uiState.value.packageVisibility)
    }

    // ─── loadApps ───

    @Test
    fun `loadApps sets isLoading true then false`() = runTest {
        whenever(repository.loadInstalledApps()).thenReturn(
            listOf(fakeAppItem("com.example.a", "App A"))
        )
        createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.loadApps()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadApps populates allApps and visibleApps`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "Alpha"),
            fakeAppItem("com.example.b", "Beta"),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.allApps.size)
        assertEquals(2, state.visibleApps.size)
        assertEquals("Alpha", state.visibleApps[0].label)
        assertEquals("Beta", state.visibleApps[1].label)
    }

    @Test
    fun `loadApps does not reload if already loaded and forceReload is false`() = runTest {
        whenever(repository.loadInstalledApps()).thenReturn(
            listOf(fakeAppItem("com.example.a", "App A"))
        )
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.allApps.size)

        whenever(repository.loadInstalledApps()).thenReturn(
            listOf(
                fakeAppItem("com.example.a", "App A"),
                fakeAppItem("com.example.b", "App B"),
            )
        )

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.allApps.size)
    }

    @Test
    fun `loadApps with forceReload fetches new data`() = runTest {
        whenever(repository.loadInstalledApps()).thenReturn(
            listOf(fakeAppItem("com.example.a", "App A"))
        )
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.allApps.size)

        whenever(repository.loadInstalledApps()).thenReturn(
            listOf(
                fakeAppItem("com.example.a", "App A"),
                fakeAppItem("com.example.b", "App B"),
            )
        )

        viewModel.loadApps(forceReload = true)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.allApps.size)
    }

    // ─── setQuery ───

    @Test
    fun `setQuery filters visibleApps by label`() = runTest {
        whenever(repository.loadInstalledApps()).thenReturn(
            listOf(
                fakeAppItem("com.example.alpha", "Alpha"),
                fakeAppItem("com.example.beta", "Beta"),
            )
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.visibleApps.size)

        viewModel.setQuery("Alp")
        advanceUntilIdle()

        assertEquals("Alp", viewModel.uiState.value.query)
        assertEquals(1, viewModel.uiState.value.visibleApps.size)
        assertEquals("Alpha", viewModel.uiState.value.visibleApps[0].label)
    }

    // ─── setSortMode ───

    @Test
    fun `setSortMode changes sort order of visibleApps`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.b", "Beta", updateTime = 200),
            fakeAppItem("com.example.a", "Alpha", updateTime = 100),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        // Default sort is UPDATE_TIME_DESC
        assertEquals("Beta", viewModel.uiState.value.visibleApps[0].label)

        viewModel.setSortMode(SortMode.NAME_ASC)
        advanceUntilIdle()

        assertEquals(SortMode.NAME_ASC, viewModel.uiState.value.sortMode)
        assertEquals("Alpha", viewModel.uiState.value.visibleApps[0].label)
        assertEquals("Beta", viewModel.uiState.value.visibleApps[1].label)
    }

    // ─── toggleApp ───

    @Test
    fun `toggleApp toggles hookEnabled for matching app`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = false),
            fakeAppItem("com.example.b", "App B", hookEnabled = false),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.allApps[0].hookEnabled)

        viewModel.toggleApp("com.example.a")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.allApps[0].hookEnabled)
        assertFalse(viewModel.uiState.value.allApps[1].hookEnabled)
    }

    @Test
    fun `toggleApp persists hook states via repository`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = false),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.toggleApp("com.example.a")
        advanceUntilIdle()

        verify(repository).persistHookStates(org.mockito.kotlin.any())
    }

    @Test
    fun `toggleApp off then on round-trips correctly`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = true),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.toggleApp("com.example.a")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.allApps[0].hookEnabled)

        viewModel.toggleApp("com.example.a")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.allApps[0].hookEnabled)
    }

    // ─── selectAll ───

    @Test
    fun `selectAll enabled sets all apps hookEnabled to true`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = false),
            fakeAppItem("com.example.b", "App B", hookEnabled = false),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.selectAll(enabled = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.allApps.all { it.hookEnabled })
    }

    @Test
    fun `selectAll disabled sets all apps hookEnabled to false`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = true),
            fakeAppItem("com.example.b", "App B", hookEnabled = true),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.selectAll(enabled = false)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.allApps.none { it.hookEnabled })
    }

    @Test
    fun `selectAll persists hook states via repository`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = false),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.selectAll(enabled = true)
        advanceUntilIdle()

        verify(repository).persistHookStates(org.mockito.kotlin.any())
    }

    // ─── setScopeMode ───

    @Test
    fun `setScopeMode persists to repository and updates state`() = runTest {
        createViewModel()
        assertFalse(viewModel.uiState.value.scopeMode)

        viewModel.setScopeMode(true)
        advanceUntilIdle()

        verify(repository).setScopeMode(true)
        assertTrue(viewModel.uiState.value.scopeMode)
    }

    // ─── setHookFilter ───

    @Test
    fun `setHookFilter filters visibleApps by hook status`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = true),
            fakeAppItem("com.example.b", "App B", hookEnabled = false),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.visibleApps.size)

        viewModel.setHookFilter(HookFilter.ON)
        advanceUntilIdle()

        assertEquals(HookFilter.ON, viewModel.uiState.value.hookFilter)
        assertEquals(1, viewModel.uiState.value.visibleApps.size)
        assertEquals("App A", viewModel.uiState.value.visibleApps[0].label)
    }

    @Test
    fun `setHookFilter OFF shows only disabled apps`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = true),
            fakeAppItem("com.example.b", "App B", hookEnabled = false),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.setHookFilter(HookFilter.OFF)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.visibleApps.size)
        assertEquals("App B", viewModel.uiState.value.visibleApps[0].label)
    }

    // ─── Error Handling ───

    @Test
    fun `loadApps propagates error when repository throws`() = runTest {
        whenever(repository.loadInstalledApps()).thenThrow(RuntimeException("Legacy load error"))
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Legacy load error", viewModel.uiState.value.errorMessage)
    }

    // ─── clearError ───

    @Test
    fun `clearError resets errorMessage to null`() = runTest {
        whenever(repository.loadInstalledApps()).thenThrow(RuntimeException("Legacy load error"))
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals("Legacy load error", viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── emptyMessage ───

    @Test
    fun `emptyMessage is null when apps exist`() = runTest {
        whenever(repository.loadInstalledApps()).thenReturn(
            listOf(fakeAppItem("com.example.a", "App A"))
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.emptyMessage)
    }

    @Test
    fun `emptyMessage is EMPTY_FILTER when query yields no results`() = runTest {
        whenever(repository.loadInstalledApps()).thenReturn(
            listOf(fakeAppItem("com.example.a", "App A"))
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.setQuery("NonExistent")
        advanceUntilIdle()

        assertEquals(ConfigViewModel.EMPTY_FILTER, viewModel.uiState.value.emptyMessage)
    }

    // ─── Helpers ───

    private fun fakeAppItem(
        packageName: String,
        label: String,
        hookEnabled: Boolean = false,
        isSystem: Boolean = false,
        installTime: Long = 0L,
        updateTime: Long = 0L,
    ): AppItem = AppItem(
        label = label,
        appInfo = ApplicationInfo().apply {
            this.packageName = packageName
            flags = if (isSystem) ApplicationInfo.FLAG_SYSTEM else 0
        },
        hookEnabled = hookEnabled,
        packageName = packageName,
        isSystem = isSystem,
        updateTime = updateTime,
        installTime = installTime,
    )
}
