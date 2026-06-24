package nota.android.crash.xp.app.observe

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.MenuItem
import android.view.Menu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.CrashSortMode
import nota.android.crash.xp.app.data.FakeCrashLogRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class CrashHistoryMenuActionsTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: FakeCrashLogRepository
    private lateinit var viewModel: CrashHistoryViewModel

    @Mock private lateinit var fragment: Fragment
    @Mock private lateinit var menu: Menu
    @Mock private lateinit var filterItem: MenuItem
    @Mock private lateinit var packageFilterItem: MenuItem
    @Mock private lateinit var exportItem: MenuItem
    @Mock private lateinit var retentionItem: MenuItem
    @Mock private lateinit var clearHistoryItem: MenuItem
    @Mock private lateinit var unknownItem: MenuItem

    private lateinit var actions: CrashHistoryMenuActions

    private lateinit var closeable: AutoCloseable

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        repository = FakeCrashLogRepository()
        viewModel = CrashHistoryViewModel(repository, ioDispatcher = testDispatcher)

        val context: Context = Robolectric.buildActivity(Activity::class.java).setup().get()
        whenever(fragment.requireContext()).thenReturn(context)
        whenever(fragment.resources).thenReturn(context.resources)

        val testLifecycleOwner = object : LifecycleOwner {
            @Suppress("DEPRECATION")
            override val lifecycle: Lifecycle =
                LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        whenever(fragment.viewLifecycleOwner).thenReturn(testLifecycleOwner)

        `when`(filterItem.itemId).thenReturn(R.id.item_observe_filter)
        `when`(packageFilterItem.itemId).thenReturn(R.id.item_observe_package_filter)
        `when`(exportItem.itemId).thenReturn(R.id.item_observe_export)
        `when`(retentionItem.itemId).thenReturn(R.id.item_observe_retention)
        `when`(clearHistoryItem.itemId).thenReturn(R.id.item_clear_history)
        `when`(unknownItem.itemId).thenReturn(-999)

        actions = CrashHistoryMenuActions(
            fragment = fragment,
            viewModel = viewModel,
            launchSaveZip = {},
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        closeable.close()
    }

    private fun menuItem(id: Int): MenuItem = when (id) {
        R.id.item_observe_filter -> filterItem
        R.id.item_observe_package_filter -> packageFilterItem
        R.id.item_observe_export -> exportItem
        R.id.item_observe_retention -> retentionItem
        R.id.item_clear_history -> clearHistoryItem
        else -> unknownItem
    }

    // ─── Filter Toggle via handleItem ───

    @Test
    fun `handleItem filter when active filter exists clears filter`() = testScope.runTest {
        viewModel.setFilter(CrashFilter(packageName = "com.example.a"))
        advanceUntilIdle()
        assertEquals(CrashFilter(packageName = "com.example.a"), viewModel.uiState.value.activeFilter)

        val handled = actions.handleItem(menuItem(R.id.item_observe_filter))

        assertTrue(handled)
        assertNull(viewModel.uiState.value.activeFilter)
    }

    @Test
    fun `handleItem filter clears filter and resets sort mode to default`() = testScope.runTest {
        viewModel.setFilter(CrashFilter(packageName = "com.example.a"))
        advanceUntilIdle()

        actions.handleItem(menuItem(R.id.item_observe_filter))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeFilter)
        // Sort mode should be preserved from the current filter
        assertEquals(CrashSortMode.TIME_NEWEST, viewModel.uiState.value.sortMode)
    }

    // ─── Package Filter ───

    @Test
    fun `handleItem package_filter returns true`() {
        val handled = actions.handleItem(menuItem(R.id.item_observe_package_filter))

        assertTrue(handled)
    }

    @Test
    fun `setPackageFilter updates activePackageFilter in uiState`() = testScope.runTest {
        viewModel.setPackageFilter("com.example.a")
        advanceUntilIdle()

        assertEquals("com.example.a", viewModel.uiState.value.activePackageFilter)
    }

    @Test
    fun `setPackageFilter null clears activePackageFilter`() = testScope.runTest {
        viewModel.setPackageFilter("com.example.a")
        advanceUntilIdle()
        assertEquals("com.example.a", viewModel.uiState.value.activePackageFilter)

        viewModel.setPackageFilter(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activePackageFilter)
    }

    // ─── Clear History Button ───

    @Test
    fun `handleItem clear_history returns true`() {
        val handled = actions.handleItem(menuItem(R.id.item_clear_history))

        assertTrue(handled)
    }

    // ─── Export Button ───

    @Test
    fun `handleItem export returns true`() {
        val handled = actions.handleItem(menuItem(R.id.item_observe_export))

        assertTrue(handled)
    }

    // ─── Retention Button ───

    @Test
    fun `handleItem retention returns true`() {
        val handled = actions.handleItem(menuItem(R.id.item_observe_retention))

        assertTrue(handled)
    }

    // ─── Unknown Item ───

    @Test
    fun `handleItem unknown item returns false`() {
        val handled = actions.handleItem(unknownItem)

        assertFalse(handled)
    }

    // ─── prepareMenu ───

    @Test
    fun `prepareMenu sets filter title when no active filter`() {
        `when`(menu.findItem(R.id.item_observe_filter)).thenReturn(filterItem)
        `when`(menu.findItem(R.id.item_observe_package_filter)).thenReturn(packageFilterItem)

        actions.prepareMenu(menu)

        verify(filterItem).setTitle(R.string.observe_menu_filter)
    }

    @Test
    fun `prepareMenu sets clear-filter title when filter is active`() = testScope.runTest {
        `when`(menu.findItem(R.id.item_observe_filter)).thenReturn(filterItem)
        `when`(menu.findItem(R.id.item_observe_package_filter)).thenReturn(packageFilterItem)
        viewModel.setFilter(CrashFilter(packageName = "com.example.a"))
        advanceUntilIdle()

        actions.prepareMenu(menu)

        verify(filterItem).setTitle(R.string.observe_menu_filter_clear)
    }

    @Test
    fun `prepareMenu sets package filter title when no active package filter`() {
        `when`(menu.findItem(R.id.item_observe_filter)).thenReturn(filterItem)
        `when`(menu.findItem(R.id.item_observe_package_filter)).thenReturn(packageFilterItem)
        whenever(fragment.getString(R.string.observe_menu_package_filter)).thenReturn("Filter by package")

        actions.prepareMenu(menu)

        verify(packageFilterItem).title = "Filter by package"
    }

    @Test
    fun `prepareMenu sets clear-package-filter title when package filter is active`() = testScope.runTest {
        `when`(menu.findItem(R.id.item_observe_filter)).thenReturn(filterItem)
        `when`(menu.findItem(R.id.item_observe_package_filter)).thenReturn(packageFilterItem)
        whenever(fragment.getString(R.string.observe_menu_package_filter_clear, "com.example.a"))
            .thenReturn("Clear filter: com.example.a")

        viewModel.setPackageFilter("com.example.a")
        advanceUntilIdle()

        actions.prepareMenu(menu)

        verify(packageFilterItem).title = "Clear filter: com.example.a"
    }
}
