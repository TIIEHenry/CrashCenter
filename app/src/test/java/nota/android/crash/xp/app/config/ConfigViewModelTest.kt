package nota.android.crash.xp.app.config

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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ManagedAppRepository
    private lateinit var visibilityRepository: PackageVisibilityRepository
    private lateinit var viewModel: ConfigViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        visibilityRepository = mock()
        whenever(repository.readHandleSystem()).thenReturn(false)
        whenever(repository.readShowSystemUi()).thenReturn(false)
        whenever(visibilityRepository.detectPackageVisibility()).thenReturn(
            PackageVisibilityHelper.Status(
                hasFullVisibility = true,
                permissionGranted = true,
                needsUserAction = false,
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects repository values`() = runTest {
        whenever(repository.readHandleSystem()).thenReturn(true)
        whenever(repository.readShowSystemUi()).thenReturn(true)

        viewModel = ConfigViewModel(repository, visibilityRepository)
        val state = viewModel.uiState.value

        assertTrue(state.handleSystem)
        assertTrue(state.showSystemUi)
        assertFalse(state.isLoading)
        assertEquals("", state.query)
        assertEquals(InterceptFilter.ALL, state.interceptFilter)
        assertTrue(state.apps.isEmpty())
        assertTrue(state.visibleApps.isEmpty())
        assertNull(state.emptyMessage)
    }

    @Test
    fun `loadApps populates apps and visibleApps`() = runTest {
        val apps = listOf(
            fakeApp("com.a", "Alpha", interceptEnabled = true),
            fakeApp("com.b", "Beta", interceptEnabled = false),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)

        viewModel = ConfigViewModel(repository, visibilityRepository)
        viewModel.loadApps()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.apps.size)
        assertEquals(2, viewModel.uiState.value.visibleApps.size)
    }

    @Test
    fun `setInterceptFilter filters visible apps`() = runTest {
        val apps = listOf(
            fakeApp("com.a", "Alpha", interceptEnabled = true),
            fakeApp("com.b", "Beta", interceptEnabled = false),
        )
        whenever(repository.loadInstalledApps()).thenReturn(apps)

        viewModel = ConfigViewModel(repository, visibilityRepository)
        viewModel.loadApps()
        advanceUntilIdle()

        viewModel.setInterceptFilter(InterceptFilter.ENABLED)
        assertEquals(1, viewModel.uiState.value.visibleApps.size)
        assertEquals("com.a", viewModel.uiState.value.visibleApps.single().packageName)
    }

    private fun fakeApp(
        packageName: String,
        label: String,
        interceptEnabled: Boolean,
    ): AppItem = AppItem(
        packageName = packageName,
        label = label,
        appInfo = android.content.pm.ApplicationInfo().apply { this.packageName = packageName },
        isSystem = false,
        interceptEnabled = interceptEnabled,
        updateTime = 0L,
        installTime = 0L,
    )
}
