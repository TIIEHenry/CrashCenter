package nota.android.crash.xp.app.shell

import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.SavedStateHandle
import nota.android.crash.xp.app.config.ConfigFragment
import nota.android.crash.xp.app.observe.ObserveHostFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ShellNavigationCoordinatorTest {

    private lateinit var viewModel: ShellViewModel
    private lateinit var navigator: ShellNavigator
    private lateinit var delegate: ShellNavigationCoordinator.Delegate
    private lateinit var coordinator: ShellNavigationCoordinator

    @Before
    fun setUp() {
        viewModel = ShellViewModel(SavedStateHandle())
        navigator = mock()
        delegate = mock()
        coordinator = ShellNavigationCoordinator(viewModel, navigator, delegate)
    }

    // ─── Tab Selection ───

    @Test
    fun `selectTab from user updates viewModel and navigates`() {
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(null)

        val switched = coordinator.selectTab(ShellTab.OBSERVE, fromUser = true)

        assertTrue(switched)
        assertEquals(ShellTab.OBSERVE, viewModel.uiState.value.selectedTab)
        verify(navigator).select(ShellTab.OBSERVE)
        verify(delegate).syncBottomNav(ShellTab.OBSERVE)
        verify(delegate).invalidateOptionsMenu()
    }

    @Test
    fun `selectTab from system does not update viewModel`() {
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(null)
        val initialTab = viewModel.uiState.value.selectedTab

        val switched = coordinator.selectTab(ShellTab.OBSERVE, fromUser = false)

        assertTrue(switched)
        assertEquals(initialTab, viewModel.uiState.value.selectedTab)
        verify(navigator).select(ShellTab.OBSERVE)
    }

    @Test
    fun `selectTab to same tab with existing fragment does not navigate`() {
        val configFragment = mock<ConfigFragment>()
        whenever(navigator.findFragment(ShellTab.CONFIG)).thenReturn(configFragment)

        val switched = coordinator.selectTab(ShellTab.CONFIG, fromUser = true)

        assertFalse(switched)
        verify(navigator, never()).select(any())
        verify(delegate).syncBottomNav(ShellTab.CONFIG)
        verify(delegate, never()).invalidateOptionsMenu()
    }

    @Test
    fun `selectTab to same tab without fragment still navigates`() {
        whenever(navigator.findFragment(ShellTab.CONFIG)).thenReturn(null)

        val switched = coordinator.selectTab(ShellTab.CONFIG, fromUser = true)

        assertTrue(switched)
        verify(navigator).select(ShellTab.CONFIG)
    }

    @Test
    fun `selectTab switches from CONFIG to OBSERVE`() {
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(null)
        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)

        coordinator.selectTab(ShellTab.OBSERVE, fromUser = true)

        assertEquals(ShellTab.OBSERVE, viewModel.uiState.value.selectedTab)
        verify(navigator).select(ShellTab.OBSERVE)
    }

    @Test
    fun `selectTab switches from OBSERVE to CONFIG`() {
        viewModel.setSelectedTab(ShellTab.OBSERVE)
        whenever(navigator.findFragment(ShellTab.CONFIG)).thenReturn(null)

        coordinator.selectTab(ShellTab.CONFIG, fromUser = true)

        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)
        verify(navigator).select(ShellTab.CONFIG)
    }

    @Test
    fun `multiple tab switches reflect latest state`() {
        whenever(navigator.findFragment(any())).thenReturn(null)

        coordinator.selectTab(ShellTab.OBSERVE, fromUser = true)
        assertEquals(ShellTab.OBSERVE, viewModel.uiState.value.selectedTab)

        coordinator.selectTab(ShellTab.CONFIG, fromUser = true)
        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)

        verify(navigator).select(ShellTab.OBSERVE)
        verify(navigator).select(ShellTab.CONFIG)
    }

    // ─── Tab State Persistence ───

    @Test
    fun `tab selection persists to SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()
        val vm = ShellViewModel(savedStateHandle)
        val coord = ShellNavigationCoordinator(vm, navigator, delegate)
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(null)

        coord.selectTab(ShellTab.OBSERVE, fromUser = true)

        assertEquals(ShellTab.OBSERVE.name, savedStateHandle.get<String>("selected_tab"))
    }

    @Test
    fun `selectTab from system does not persist tab`() {
        val savedStateHandle = SavedStateHandle()
        val vm = ShellViewModel(savedStateHandle)
        val coord = ShellNavigationCoordinator(vm, navigator, delegate)
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(null)

        coord.selectTab(ShellTab.OBSERVE, fromUser = false)

        assertNull(savedStateHandle.get<String>("selected_tab"))
    }

    // ─── Back Navigation ───

    @Test
    fun `handleBackNavigation on OBSERVE switches to CONFIG and consumes event`() {
        viewModel.setSelectedTab(ShellTab.OBSERVE)
        whenever(navigator.findFragment(ShellTab.CONFIG)).thenReturn(null)

        val consumed = coordinator.handleBackNavigation()

        assertTrue(consumed)
        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `handleBackNavigation on CONFIG does not consume event`() {
        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)

        val consumed = coordinator.handleBackNavigation()

        assertFalse(consumed)
        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `handleBackNavigation twice from OBSERVE goes to CONFIG then delegates`() {
        viewModel.setSelectedTab(ShellTab.OBSERVE)
        whenever(navigator.findFragment(any())).thenReturn(null)

        // First back press: OBSERVE -> CONFIG
        val first = coordinator.handleBackNavigation()
        assertTrue(first)
        assertEquals(ShellTab.CONFIG, viewModel.uiState.value.selectedTab)

        // Second back press: CONFIG -> delegate to system
        val second = coordinator.handleBackNavigation()
        assertFalse(second)
    }

    // ─── Options Menu ───

    @Test
    fun `optionsMenuResForTab returns correct resources`() {
        assertEquals(nota.android.crash.xp.app.R.menu.menu_main,
            coordinator.optionsMenuResForTab(ShellTab.CONFIG))
        assertEquals(nota.android.crash.xp.app.R.menu.menu_observe,
            coordinator.optionsMenuResForTab(ShellTab.OBSERVE))
    }

    @Test
    fun `prepareOptionsMenu on CONFIG delegates to ConfigFragment`() {
        val configFragment = mock<ConfigFragment>()
        val menu = mock<Menu>()
        whenever(navigator.findFragment(ShellTab.CONFIG)).thenReturn(configFragment)

        coordinator.prepareOptionsMenu(menu)

        verify(configFragment).prepareOptionsMenu(menu)
    }

    @Test
    fun `prepareOptionsMenu on CONFIG with no fragment does nothing`() {
        val menu = mock<Menu>()
        whenever(navigator.findFragment(ShellTab.CONFIG)).thenReturn(null)

        coordinator.prepareOptionsMenu(menu)

        verify(menu, never()).size()
    }

    @Test
    fun `prepareOptionsMenu on OBSERVE delegates to ObserveHostFragment`() {
        viewModel.setSelectedTab(ShellTab.OBSERVE)
        val observeHost = mock<ObserveHostFragment>()
        val menu = mock<Menu>()
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(observeHost)

        coordinator.prepareOptionsMenu(menu)

        verify(observeHost).prepareOptionsMenu(menu)
    }

    @Test
    fun `prepareOptionsMenu on OBSERVE with no fragment does nothing`() {
        viewModel.setSelectedTab(ShellTab.OBSERVE)
        val menu = mock<Menu>()
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(null)

        coordinator.prepareOptionsMenu(menu)

        verify(menu, never()).size()
    }

    @Test
    fun `onOptionsItemSelected delegates to ConfigFragment`() {
        val configFragment = mock<ConfigFragment>()
        val item = mock<MenuItem>()
        whenever(navigator.findFragment(ShellTab.CONFIG)).thenReturn(configFragment)
        whenever(configFragment.handleOptionsItem(item)).thenReturn(true)

        val handled = coordinator.onOptionsItemSelected(item)

        assertTrue(handled)
        verify(configFragment).handleOptionsItem(item)
    }

    @Test
    fun `onOptionsItemSelected returns false when no ConfigFragment`() {
        val item = mock<MenuItem>()
        whenever(navigator.findFragment(ShellTab.CONFIG)).thenReturn(null)

        val handled = coordinator.onOptionsItemSelected(item)

        assertFalse(handled)
    }

    @Test
    fun `onOptionsItemSelected returns false when ConfigFragment does not handle`() {
        val configFragment = mock<ConfigFragment>()
        val item = mock<MenuItem>()
        whenever(navigator.findFragment(ShellTab.CONFIG)).thenReturn(configFragment)
        whenever(configFragment.handleOptionsItem(item)).thenReturn(false)

        val handled = coordinator.onOptionsItemSelected(item)

        assertFalse(handled)
    }

    @Test
    fun `onOptionsItemSelected on OBSERVE delegates to ObserveHostFragment`() {
        viewModel.setSelectedTab(ShellTab.OBSERVE)
        val observeHost = mock<ObserveHostFragment>()
        val item = mock<MenuItem>()
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(observeHost)
        whenever(observeHost.handleOptionsItem(item)).thenReturn(true)

        val handled = coordinator.onOptionsItemSelected(item)

        assertTrue(handled)
        verify(observeHost).handleOptionsItem(item)
    }

    @Test
    fun `onOptionsItemSelected on OBSERVE returns false when no ObserveHostFragment`() {
        viewModel.setSelectedTab(ShellTab.OBSERVE)
        val item = mock<MenuItem>()
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(null)

        val handled = coordinator.onOptionsItemSelected(item)

        assertFalse(handled)
    }

    @Test
    fun `onOptionsItemSelected on OBSERVE returns false when ObserveHostFragment does not handle`() {
        viewModel.setSelectedTab(ShellTab.OBSERVE)
        val observeHost = mock<ObserveHostFragment>()
        val item = mock<MenuItem>()
        whenever(navigator.findFragment(ShellTab.OBSERVE)).thenReturn(observeHost)
        whenever(observeHost.handleOptionsItem(item)).thenReturn(false)

        val handled = coordinator.onOptionsItemSelected(item)

        assertFalse(handled)
    }

    // ─── Current Tab ───

    @Test
    fun `currentTab returns viewModel selected tab`() {
        assertEquals(ShellTab.CONFIG, coordinator.currentTab())

        viewModel.setSelectedTab(ShellTab.OBSERVE)
        assertEquals(ShellTab.OBSERVE, coordinator.currentTab())
    }
}
