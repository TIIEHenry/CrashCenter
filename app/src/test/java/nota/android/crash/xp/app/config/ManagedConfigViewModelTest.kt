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
class ManagedConfigViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: ManagedAppRepository
    private lateinit var visibilityRepository: PackageVisibilityRepository
    private lateinit var viewModel: ManagedConfigViewModel

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ManagedConfigViewModel(repository, visibilityRepository, CoroutineScope(testDispatcher))
    }

    // ─── Initial State ───

    @Test
    fun `initial state is not legacy mode`() = runTest {
        createViewModel()
        assertFalse(viewModel.uiState.value.isLegacyMode)
    }

    @Test
    fun `initial state has empty app lists`() = runTest {
        createViewModel()
        val state = viewModel.uiState.value
        assertTrue(state.managedApps.isEmpty())
        assertTrue(state.visibleManagedApps.isEmpty())
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
    fun `loadApps calls repository pruneUninstalled and loadManagedApps`() = runTest {
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        verify(repository).pruneUninstalled()
        verify(repository).loadManagedApps()
    }

    @Test
    fun `loadApps sets isLoading true then false`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(fakeManagedApp("com.example.a", "App A"))
        )
        createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.loadApps()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadApps populates managedApps and visibleManagedApps`() = runTest {
        val apps = listOf(
            fakeManagedApp("com.example.a", "Alpha"),
            fakeManagedApp("com.example.b", "Beta"),
        )
        whenever(repository.loadManagedApps()).thenReturn(apps)
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.managedApps.size)
        assertEquals(2, state.visibleManagedApps.size)
        assertEquals("Alpha", state.visibleManagedApps[0].label)
        assertEquals("Beta", state.visibleManagedApps[1].label)
    }

    @Test
    fun `loadApps does not reload if already loaded and forceReload is false`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(fakeManagedApp("com.example.a", "App A"))
        )
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.managedApps.size)

        whenever(repository.loadManagedApps()).thenReturn(
            listOf(
                fakeManagedApp("com.example.a", "App A"),
                fakeManagedApp("com.example.b", "App B"),
            )
        )

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.managedApps.size)
    }

    @Test
    fun `loadApps with forceReload fetches new data`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(fakeManagedApp("com.example.a", "App A"))
        )
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.managedApps.size)

        whenever(repository.loadManagedApps()).thenReturn(
            listOf(
                fakeManagedApp("com.example.a", "App A"),
                fakeManagedApp("com.example.b", "App B"),
            )
        )

        viewModel.loadApps(forceReload = true)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.managedApps.size)
    }

    // ─── setQuery ───

    @Test
    fun `setQuery filters visibleManagedApps by label`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(
                fakeManagedApp("com.example.alpha", "Alpha"),
                fakeManagedApp("com.example.beta", "Beta"),
            )
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.visibleManagedApps.size)

        viewModel.setQuery("Alp")
        advanceUntilIdle()

        assertEquals("Alp", viewModel.uiState.value.query)
        assertEquals(1, viewModel.uiState.value.visibleManagedApps.size)
        assertEquals("Alpha", viewModel.uiState.value.visibleManagedApps[0].label)
    }

    // ─── setSortMode ───

    @Test
    fun `setSortMode changes sort order of visibleManagedApps`() = runTest {
        val apps = listOf(
            fakeManagedApp("com.example.b", "Beta", updateTime = 200),
            fakeManagedApp("com.example.a", "Alpha", updateTime = 100),
        )
        whenever(repository.loadManagedApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        // Default sort is UPDATE_TIME_DESC
        assertEquals("Beta", viewModel.uiState.value.visibleManagedApps[0].label)

        viewModel.setSortMode(SortMode.NAME_ASC)
        advanceUntilIdle()

        assertEquals(SortMode.NAME_ASC, viewModel.uiState.value.sortMode)
        assertEquals("Alpha", viewModel.uiState.value.visibleManagedApps[0].label)
        assertEquals("Beta", viewModel.uiState.value.visibleManagedApps[1].label)
    }

    // ─── setManagedFilter ───

    @Test
    fun `setManagedFilter filters visibleManagedApps by status`() = runTest {
        val apps = listOf(
            fakeManagedApp("com.example.a", "App A", switchChecked = true),
            fakeManagedApp("com.example.b", "App B", switchChecked = false),
        )
        whenever(repository.loadManagedApps()).thenReturn(apps)
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.visibleManagedApps.size)

        viewModel.setManagedFilter(ManagedFilter.ENABLED)
        advanceUntilIdle()

        assertEquals(ManagedFilter.ENABLED, viewModel.uiState.value.managedFilter)
        assertEquals(1, viewModel.uiState.value.visibleManagedApps.size)
        assertEquals("App A", viewModel.uiState.value.visibleManagedApps[0].label)
    }

    // ─── setManagedSwitch ───

    @Test
    fun `setManagedSwitch calls repository setInterventionEnabled`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(fakeManagedApp("com.example.a", "App A", switchChecked = false))
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.setManagedSwitch("com.example.a", enabled = true)
        advanceUntilIdle()

        verify(repository).setInterventionEnabled("com.example.a", true)
    }

    @Test
    fun `setManagedSwitch updates app switchChecked in state`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(fakeManagedApp("com.example.a", "App A", switchChecked = false))
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.managedApps[0].switchChecked)

        // After setManagedSwitch calls repository, reloadManagedApp re-fetches from repository
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(fakeManagedApp("com.example.a", "App A", switchChecked = true))
        )

        viewModel.setManagedSwitch("com.example.a", enabled = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.managedApps[0].switchChecked)
    }

    // ─── addManagedPackages ───

    @Test
    fun `addManagedPackages calls repository and triggers force reload`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(fakeManagedApp("com.example.a", "App A"))
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.managedApps.size)

        whenever(repository.loadManagedApps()).thenReturn(
            listOf(
                fakeManagedApp("com.example.a", "App A"),
                fakeManagedApp("com.example.b", "App B"),
            )
        )

        viewModel.addManagedPackages(listOf("com.example.b"))
        advanceUntilIdle()

        verify(repository).addManagedPackages(listOf("com.example.b"))
        assertEquals(2, viewModel.uiState.value.managedApps.size)
    }

    @Test
    fun `addManagedPackages with empty list does nothing`() = runTest {
        createViewModel()

        viewModel.addManagedPackages(emptyList())
        advanceUntilIdle()
    }

    // ─── Error Handling ───

    @Test
    fun `loadApps propagates error when repository throws`() = runTest {
        whenever(repository.loadManagedApps()).thenThrow(RuntimeException("Managed load error"))
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Managed load error", viewModel.uiState.value.errorMessage)
    }

    // ─── clearError ───

    @Test
    fun `clearError resets errorMessage to null`() = runTest {
        whenever(repository.loadManagedApps()).thenThrow(RuntimeException("Managed load error"))
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals("Managed load error", viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── emptyMessage ───

    @Test
    fun `emptyMessage is null when managed apps exist`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(fakeManagedApp("com.example.a", "App A"))
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.emptyMessage)
    }

    @Test
    fun `emptyMessage is EMPTY_FILTER when query yields no results`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(
            listOf(fakeManagedApp("com.example.a", "App A"))
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.setQuery("NonExistent")
        advanceUntilIdle()

        assertEquals(ConfigViewModel.EMPTY_FILTER, viewModel.uiState.value.emptyMessage)
    }

    @Test
    fun `emptyMessage is EMPTY_MANAGED_LIST when no managed apps exist`() = runTest {
        whenever(repository.loadManagedApps()).thenReturn(emptyList())
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertEquals(ConfigViewModel.EMPTY_MANAGED_LIST, viewModel.uiState.value.emptyMessage)
    }

    // ─── Helpers ───

    private fun fakeManagedApp(
        packageName: String,
        label: String,
        switchChecked: Boolean = false,
        isSystem: Boolean = false,
        updateTime: Long = 0L,
        installTime: Long = 0L,
    ): ManagedApp = ManagedApp(
        packageName = packageName,
        label = label,
        appInfo = ApplicationInfo().apply {
            this.packageName = packageName
            flags = if (isSystem) ApplicationInfo.FLAG_SYSTEM else 0
        },
        isSystem = isSystem,
        interventionStatus = if (switchChecked) InterventionStatus.ENABLED else InterventionStatus.PENDING,
        switchChecked = switchChecked,
        enabledRuleCount = if (switchChecked) 1 else 0,
        summary = null,
        updateTime = updateTime,
        installTime = installTime,
    )
}
