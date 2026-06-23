package nota.android.crash.xp.app.shell

import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.CallbackSuppressor
import nota.android.crash.xp.app.databinding.ActivityMainShellBinding

/**
 * Encapsulates bottom navigation setup, tab selection handling, and tab state persistence
 * for [MainShellActivity]. Delegates fragment transactions to [ShellNavigator] and state
 * to [ShellViewModel].
 *
 * The actual navigation decisions are forwarded to [ShellNavigationCoordinator] so that
 * they can be unit-tested without an Activity.
 */
class ShellTabController(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainShellBinding,
    private val viewModel: ShellViewModel,
    private val navigator: ShellNavigator,
) {

    private val coordinator: ShellNavigationCoordinator
    private val navSuppressor = CallbackSuppressor()

    init {
        coordinator = ShellNavigationCoordinator(
            viewModel = viewModel,
            navigator = navigator,
            delegate = object : ShellNavigationCoordinator.Delegate {
                override fun syncBottomNav(tab: ShellTab) {
                    val menuItemId = when (tab) {
                        ShellTab.CONFIG -> R.id.nav_config
                        ShellTab.OBSERVE -> R.id.nav_observe
                    }
                    if (binding.bottomNav.selectedItemId != menuItemId) {
                        navSuppressor.run {
                            binding.bottomNav.selectedItemId = menuItemId
                        }
                    }
                }

                override fun invalidateOptionsMenu() {
                    activity.invalidateOptionsMenu()
                }
            },
        )
    }

    fun onCreate(initialTab: ShellTab) {
        setupBottomNav()
        setupBackNavigation()

        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // React to state changes if needed beyond initial setup
                }
            }
        }

        coordinator.selectTab(initialTab, fromUser = false)
    }

    fun onResume() {
        viewModel.refreshXposedStatus(activity)
    }

    fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        val tab = coordinator.currentTab()
        val menuRes = coordinator.optionsMenuResForTab(tab)
        return if (menuRes != null) {
            activity.menuInflater.inflate(menuRes, menu)
            true
        } else {
            menu.clear()
            true
        }
    }

    fun onPrepareOptionsMenu(menu: android.view.Menu): Boolean {
        coordinator.prepareOptionsMenu(menu)
        return true
    }

    fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return coordinator.onOptionsItemSelected(item)
    }

    fun selectShellTab(tab: ShellTab) {
        coordinator.selectTab(tab, fromUser = true)
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (navSuppressor.suppressed) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.nav_config -> {
                    coordinator.selectTab(ShellTab.CONFIG, fromUser = true)
                    true
                }
                R.id.nav_observe -> {
                    coordinator.selectTab(ShellTab.OBSERVE, fromUser = true)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackNavigation() {
        activity.onBackPressedDispatcher.addCallback(
            activity,
            true,
        ) {
            if (!coordinator.handleBackNavigation()) {
                isEnabled = false
                activity.onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}

private fun ShellViewModel.refreshXposedStatus(activity: AppCompatActivity) {
    refreshXposedStatus(nota.android.crash.xp.app.ModuleActivation.isModuleActive())
}
