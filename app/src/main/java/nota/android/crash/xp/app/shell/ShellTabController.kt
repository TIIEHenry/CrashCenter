package nota.android.crash.xp.app.shell

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.get
import androidx.core.view.size
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.config.ConfigFragment
import nota.android.crash.xp.app.databinding.ActivityMainShellBinding

/**
 * Encapsulates bottom navigation setup, tab selection handling, and tab state persistence
 * for [MainShellActivity]. Delegates fragment transactions to [ShellNavigator] and state
 * to [ShellViewModel].
 */
class ShellTabController(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainShellBinding,
    private val viewModel: ShellViewModel,
    private val navigator: ShellNavigator,
) {

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

        selectTab(initialTab, fromUser = false)
    }

    fun onResume() {
        viewModel.refreshXposedStatus(activity)
    }

    fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        return when (viewModel.uiState.value.selectedTab) {
            ShellTab.CONFIG -> {
                activity.menuInflater.inflate(R.menu.menu_main, menu)
                true
            }
            ShellTab.OBSERVE -> {
                activity.menuInflater.inflate(R.menu.menu_observe_stub, menu)
                true
            }
            else -> {
                menu.clear()
                true
            }
        }
    }

    fun onPrepareOptionsMenu(menu: android.view.Menu): Boolean {
        when (viewModel.uiState.value.selectedTab) {
            ShellTab.CONFIG -> {
                (navigator.findFragment(ShellTab.CONFIG) as? ConfigFragment)
                    ?.prepareOptionsMenu(menu)
            }
            ShellTab.OBSERVE -> {
                for (index in 0 until menu.size) {
                    menu[index].isEnabled = false
                }
            }
            else -> Unit
        }
        return true
    }

    fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        val configFragment = navigator.findFragment(ShellTab.CONFIG) as? ConfigFragment
        return configFragment?.handleOptionsItem(item) == true
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_config -> {
                    selectTab(ShellTab.CONFIG, fromUser = true)
                    true
                }
                R.id.nav_observe -> {
                    selectTab(ShellTab.OBSERVE, fromUser = true)
                    true
                }
                else -> false
            }
        }
    }

    private fun selectTab(tab: ShellTab, fromUser: Boolean) {
        val currentTab = viewModel.uiState.value.selectedTab
        if (currentTab == tab && navigator.findFragment(tab) != null) {
            syncBottomNav(tab)
            return
        }

        if (fromUser) {
            viewModel.setSelectedTab(tab)
        }

        navigator.select(tab)
        syncBottomNav(tab)
        activity.invalidateOptionsMenu()
    }

    private fun syncBottomNav(tab: ShellTab) {
        val menuItemId = when (tab) {
            ShellTab.CONFIG -> R.id.nav_config
            ShellTab.OBSERVE -> R.id.nav_observe
        }
        if (binding.bottomNav.selectedItemId != menuItemId) {
            binding.bottomNav.selectedItemId = menuItemId
        }
    }

    private fun setupBackNavigation() {
        activity.onBackPressedDispatcher.addCallback(
            activity,
            true,
        ) {
            if (viewModel.uiState.value.selectedTab == ShellTab.OBSERVE) {
                selectTab(ShellTab.CONFIG, fromUser = true)
                return@addCallback
            }
            isEnabled = false
            activity.onBackPressedDispatcher.onBackPressed()
        }
    }
}

private fun ShellViewModel.refreshXposedStatus(activity: AppCompatActivity) {
    refreshXposedStatus(nota.android.crash.xp.app.ModuleActivation.isModuleActive())
}
