package nota.android.crash.xp.app.shell

import androidx.fragment.app.Fragment
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.config.ConfigFragment

/**
 * Testable coordinator that encapsulates the navigation decisions (tab selection,
 * back navigation, options menu logic) for the shell.  Android-framework glue
 * (BottomNavigationView, Activity#invalidateOptionsMenu, FragmentManager) is
 * injected via [navigator] and the caller-provided [delegate].
 */
class ShellNavigationCoordinator(
    private val viewModel: ShellViewModel,
    private val navigator: ShellNavigator,
    private val delegate: Delegate,
) {

    interface Delegate {
        /** Sync the BottomNavigationView to reflect [tab]. */
        fun syncBottomNav(tab: ShellTab)
        /** Ask the host Activity to rebuild its options menu. */
        fun invalidateOptionsMenu()
    }

    // ─── Tab selection ───

    /**
     * Switch to [tab].  When [fromUser] is true the ViewModel is updated so the
     * new tab is persisted; when false (e.g. restore on launch) the ViewModel is
     * left unchanged.
     *
     * Returns `true` when a fragment transaction was actually performed.
     */
    fun selectTab(tab: ShellTab, fromUser: Boolean): Boolean {
        val currentTab = viewModel.uiState.value.selectedTab
        if (currentTab == tab && navigator.findFragment(tab) != null) {
            delegate.syncBottomNav(tab)
            return false
        }

        if (fromUser) {
            viewModel.setSelectedTab(tab)
        }

        navigator.select(tab)
        delegate.syncBottomNav(tab)
        delegate.invalidateOptionsMenu()
        return true
    }

    /** @return the currently selected tab from the ViewModel. */
    fun currentTab(): ShellTab = viewModel.uiState.value.selectedTab

    // ─── Back navigation ───

    /**
     * Handle a back-press event.
     *
     * Returns `true` if the event was consumed (e.g. switched from OBSERVE to
     * CONFIG). Returns `false` when the caller should forward the event to the
     * system back dispatcher.
     */
    fun handleBackNavigation(): Boolean {
        return if (viewModel.uiState.value.selectedTab == ShellTab.OBSERVE) {
            selectTab(ShellTab.CONFIG, fromUser = true)
            true
        } else {
            false
        }
    }

    // ─── Options menu ───

    /** @return the menu resource to inflate for [tab], or `null` to clear. */
    fun optionsMenuResForTab(tab: ShellTab): Int? = when (tab) {
        ShellTab.CONFIG -> R.menu.menu_main
        ShellTab.OBSERVE -> R.menu.menu_observe_stub
    }

    /**
     * Prepare the options menu for the current tab.
     *
     * For [ShellTab.CONFIG] the menu is forwarded to the existing
     * [ConfigFragment] so it can show/hide items based on its own state.
     * For [ShellTab.OBSERVE] all items are disabled.
     */
    fun prepareOptionsMenu(menu: android.view.Menu) {
        when (viewModel.uiState.value.selectedTab) {
            ShellTab.CONFIG -> {
                (navigator.findFragment(ShellTab.CONFIG) as? ConfigFragment)
                    ?.prepareOptionsMenu(menu)
            }
            ShellTab.OBSERVE -> {
                for (index in 0 until menu.size()) {
                    menu.getItem(index).isEnabled = false
                }
            }
        }
    }

    /**
     * Attempt to delegate an options-item selection to the [ConfigFragment].
     *
     * Returns `true` when the item was handled.
     */
    fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        val configFragment = navigator.findFragment(ShellTab.CONFIG) as? ConfigFragment
        return configFragment?.handleOptionsItem(item) == true
    }
}
