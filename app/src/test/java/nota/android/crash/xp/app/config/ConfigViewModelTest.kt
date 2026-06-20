package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeAppRepository
    private lateinit var viewModel: ConfigViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeAppRepository()
        repository.legacyMode = true
        repository._scopeMode = false
        repository._handleSystem = false
        repository._showSystemUi = false
        repository.packageVisibilityStatus = PackageVisibilityHelper.Status(
            hasFullVisibility = true,
            permissionGranted = true,
            needsUserAction = false,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ConfigViewModel(repository)
    }

    // ─── Initial State ───

    @Test
    fun `initial state reflects repository values`() = runTest {
        repository._scopeMode = true
        repository._handleSystem = true
        repository._showSystemUi = true
        repository.legacyMode = false

        createViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.scopeMode)
        assertTrue(state.handleSystem)
        assertTrue(state.showSystemUi)
        assertFalse(state.isLegacyMode)
        assertFalse(state.isLoading)
        assertEquals("", state.query)
        assertEquals(HookFilter.ALL, state.hookFilter)
        assertEquals(ManagedFilter.ALL, state.managedFilter)
        assertTrue(state.allApps.isEmpty())
        assertTrue(state.visibleApps.isEmpty())
        assertTrue(state.managedApps.isEmpty())
        assertNull(state.emptyMessage)
    }

    @Test
    fun `initial state includes package visibility`() = runTest {
        val status = PackageVisibilityHelper.Status(
            hasFullVisibility = false,
            permissionGranted = false,
            needsUserAction = true,
            visiblePackageCount = 5,
        )
        repository.packageVisibilityStatus = status

        createViewModel()

        assertEquals(status, viewModel.uiState.value.packageVisibility)
    }

    // ─── Loading Toggles ───

    @Test
    fun `loadApps sets isLoading to true then false`() = runTest {
        repository.installedApps = listOf(
            fakeAppItem("com.example.a", "App A"),
            fakeAppItem("com.example.b", "App B"),
        )
        createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.loadApps()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadApps in managed mode sets isLoading to true then false`() = runTest {
        repository.legacyMode = false
        repository.managedAppsList = listOf(
            fakeManagedApp("com.example.a", "App A"),
            fakeManagedApp("com.example.b", "App B"),
        )
        createViewModel()

        viewModel.loadApps()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── State Updates After loadApps ───

    @Test
    fun `loadApps populates allApps and visibleApps in legacy mode`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "Alpha"),
            fakeAppItem("com.example.b", "Beta"),
        )
        repository.installedApps = apps
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.allApps.size)
        assertEquals(2, state.visibleApps.size)
        assertEquals("Alpha", state.visibleApps[0].name)
        assertEquals("Beta", state.visibleApps[1].name)
    }

    @Test
    fun `loadApps populates managedApps and visibleManagedApps in managed mode`() = runTest {
        repository.legacyMode = false
        val apps = listOf(
            fakeManagedApp("com.example.a", "Alpha"),
            fakeManagedApp("com.example.b", "Beta"),
        )
        repository.managedAppsList = apps
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
        val apps = listOf(fakeAppItem("com.example.a", "App A"))
        repository.installedApps = apps
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.allApps.size)

        // Change repository data after first load
        repository.installedApps = listOf(
            fakeAppItem("com.example.a", "App A"),
            fakeAppItem("com.example.b", "App B"),
        )

        // Without forceReload, should not re-fetch
        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.allApps.size)
    }

    @Test
    fun `loadApps with forceReload fetches new data`() = runTest {
        val apps = listOf(fakeAppItem("com.example.a", "App A"))
        repository.installedApps = apps
        createViewModel()

        viewModel.loadApps()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.allApps.size)

        repository.installedApps = listOf(
            fakeAppItem("com.example.a", "App A"),
            fakeAppItem("com.example.b", "App B"),
        )

        viewModel.loadApps(forceReload = true)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.allApps.size)
    }

    // ─── Generation / Cancellation Logic ───

    @Test
    fun `consecutive loadApps cancels previous generation`() = runTest {
        val apps1 = listOf(fakeAppItem("com.example.a", "App A"))
        val apps2 = listOf(
            fakeAppItem("com.example.b", "App B"),
            fakeAppItem("com.example.c", "App C"),
        )
        repository.installedApps = apps1
        createViewModel()

        // Start first load
        viewModel.loadApps()
        assertTrue(viewModel.uiState.value.isLoading)

        // Change repository and start second load before first completes
        repository.installedApps = apps2
        viewModel.loadApps(forceReload = true)

        advanceUntilIdle()

        // Second load should win; first load's result is discarded
        val state = viewModel.uiState.value
        assertEquals(2, state.allApps.size)
        assertEquals("App B", state.allApps[0].name)
        assertEquals("App C", state.allApps[1].name)
    }

    @Test
    fun `loadApps handles exception gracefully`() = runTest {
        // Use a repository that throws on loadInstalledApps
        val throwingRepo = object : AppRepositoryInterface {
            override fun isLegacyMode(): Boolean = true
            override fun readScopeMode(): Boolean = false
            override fun readHandleSystem(): Boolean = false
            override fun readShowSystemUi(): Boolean = false
            override fun setScopeMode(enabled: Boolean) {}
            override fun setHandleSystem(enabled: Boolean) {}
            override fun setShowSystemUi(enabled: Boolean) {}
            override fun detectPackageVisibility(): PackageVisibilityHelper.Status =
                PackageVisibilityHelper.Status(true, true, false)
            override fun detectPackageVisibilityAfterLoad(loadedCount: Int): PackageVisibilityHelper.Status =
                PackageVisibilityHelper.Status(true, true, false)
            override fun loadInstalledApps(): Flow<List<AppItem>> = flow {
                throw RuntimeException("Simulated error")
            }
            override fun persistHookStates(apps: List<AppItem>) {}
            override fun loadManagedApps(): Flow<List<ManagedApp>> = flow { emit(emptyList()) }
            override fun loadPickableApps(): Flow<List<PickableApp>> = flow { emit(emptyList()) }
            override fun readManagedPackageNames(): Set<String>? = null
            override fun addManagedPackages(packages: Collection<String>) {}
            override fun removeManagedPackage(packageName: String) {}
            override fun pruneUninstalled(): Int = 0
            override fun getProfile(packageName: String): AppInterventionProfile = AppInterventionProfile.EMPTY
            override fun saveProfile(packageName: String, profile: AppInterventionProfile) {}
            override fun setInterventionEnabled(packageName: String, enabled: Boolean) {}
        }
        viewModel = ConfigViewModel(throwingRepo)

        viewModel.loadApps()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── State Mutations ───

    @Test
    fun `setQuery updates query and filters visible apps`() = runTest {
        repository.installedApps = listOf(
            fakeAppItem("com.example.alpha", "Alpha"),
            fakeAppItem("com.example.beta", "Beta"),
        )
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.visibleApps.size)

        viewModel.setQuery("Alp")
        advanceUntilIdle()

        assertEquals("Alp", viewModel.uiState.value.query)
        assertEquals(1, viewModel.uiState.value.visibleApps.size)
        assertEquals("Alpha", viewModel.uiState.value.visibleApps[0].name)
    }

    @Test
    fun `setHookFilter filters visible apps`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = true),
            fakeAppItem("com.example.b", "App B", hookEnabled = false),
        )
        repository.installedApps = apps
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.setHookFilter(HookFilter.ON)
        advanceUntilIdle()

        assertEquals(HookFilter.ON, viewModel.uiState.value.hookFilter)
        assertEquals(1, viewModel.uiState.value.visibleApps.size)
        assertEquals("App A", viewModel.uiState.value.visibleApps[0].name)
    }

    @Test
    fun `toggleApp toggles hookEnabled and persists`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = false),
        )
        repository.installedApps = apps
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.allApps[0].hookEnabled)

        viewModel.toggleApp("com.example.a")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.allApps[0].hookEnabled)
        assertTrue(repository.getPersistedHookStates()?.get(0)?.hookEnabled == true)
    }

    @Test
    fun `setScopeMode updates repository and state`() = runTest {
        createViewModel()
        assertFalse(viewModel.uiState.value.scopeMode)

        viewModel.setScopeMode(true)
        assertTrue(viewModel.uiState.value.scopeMode)
        assertTrue(repository._scopeMode)
    }

    @Test
    fun `setHandleSystem updates repository and state`() = runTest {
        createViewModel()
        assertFalse(viewModel.uiState.value.handleSystem)

        viewModel.setHandleSystem(true)
        assertTrue(viewModel.uiState.value.handleSystem)
        assertTrue(repository._handleSystem)
    }

    @Test
    fun `setShowSystemUi updates repository and state`() = runTest {
        createViewModel()
        assertFalse(viewModel.uiState.value.showSystemUi)

        viewModel.setShowSystemUi(true)
        assertTrue(viewModel.uiState.value.showSystemUi)
        assertTrue(repository._showSystemUi)
    }

    @Test
    fun `selectAll sets all apps hookEnabled`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.a", "App A", hookEnabled = false),
            fakeAppItem("com.example.b", "App B", hookEnabled = false),
        )
        repository.installedApps = apps
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.selectAll(enabled = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.allApps.all { it.hookEnabled })
        val persisted = repository.getPersistedHookStates()
        assertEquals(2, persisted?.size)
        assertTrue(persisted?.all { it.hookEnabled } == true)
    }

    @Test
    fun `setSortMode sorts visible apps`() = runTest {
        val apps = listOf(
            fakeAppItem("com.example.b", "Beta", installTime = 200, updateTime = 200),
            fakeAppItem("com.example.a", "Alpha", installTime = 100, updateTime = 100),
        )
        repository.installedApps = apps
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        // Default sort is UPDATE_TIME_DESC, so Beta should be first
        assertEquals("Beta", viewModel.uiState.value.visibleApps[0].name)

        viewModel.setSortMode(SortMode.NAME_ASC)
        advanceUntilIdle()

        assertEquals(SortMode.NAME_ASC, viewModel.uiState.value.sortMode)
        assertEquals("Alpha", viewModel.uiState.value.visibleApps[0].name)
        assertEquals("Beta", viewModel.uiState.value.visibleApps[1].name)
    }

    @Test
    fun `setManagedSwitch updates managed app in managed mode`() = runTest {
        repository.legacyMode = false
        repository.setProfile("com.example.a", AppInterventionProfile(
            rules = listOf(InterventionRule.defaultCatchAll(enabled = false)),
        ))
        val apps = listOf(
            fakeManagedApp("com.example.a", "App A", switchChecked = false),
        )
        repository.managedAppsList = apps
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.managedApps[0].switchChecked)

        viewModel.setManagedSwitch("com.example.a", enabled = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.managedApps[0].switchChecked)
        assertTrue(repository.isInterventionEnabled("com.example.a"))
    }

    @Test
    fun `addManagedPackages triggers force reload`() = runTest {
        repository.legacyMode = false
        repository.managedAppsList = listOf(fakeManagedApp("com.example.a", "App A"))
        createViewModel()
        viewModel.loadApps()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.managedApps.size)

        repository.managedAppsList = listOf(
            fakeManagedApp("com.example.a", "App A"),
            fakeManagedApp("com.example.b", "App B"),
        )

        viewModel.addManagedPackages(listOf("com.example.b"))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.managedApps.size)
    }

    // ─── Helpers ───

    private fun fakeAppItem(
        packageName: String,
        name: String,
        hookEnabled: Boolean = false,
        isSystemApp: Boolean = false,
        installTime: Long = 0L,
        updateTime: Long = 0L,
    ): AppItem = AppItem(
        name = name,
        appInfo = ApplicationInfo().apply {
            this.packageName = packageName
            flags = if (isSystemApp) ApplicationInfo.FLAG_SYSTEM else 0
        },
        hookEnabled = hookEnabled,
        packageName = packageName,
        isSystemApp = isSystemApp,
        updateTime = updateTime,
        installTime = installTime,
    )

    private fun fakeManagedApp(
        packageName: String,
        label: String,
        switchChecked: Boolean = false,
        isSystem: Boolean = false,
        installTime: Long = 0L,
        updateTime: Long = 0L,
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
