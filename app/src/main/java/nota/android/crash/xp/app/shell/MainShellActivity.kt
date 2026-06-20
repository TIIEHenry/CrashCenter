package nota.android.crash.xp.app.shell

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.core.view.get
import androidx.core.view.size

import nota.android.crash.xp.PrefMigrator
import nota.android.crash.xp.XposedManagerLauncher
import nota.android.crash.xp.app.ModuleActivation
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.SystemBars
import nota.android.crash.xp.app.common.ui.StatusBanner
import nota.android.crash.xp.app.common.ui.ToolbarHeaderInsets
import nota.android.crash.xp.app.config.ConfigFragment
import nota.android.crash.xp.app.databinding.ActivityMainShellBinding
import nota.android.crash.xp.app.observe.ObserveHostFragment

class MainShellActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainShellBinding
    private val shellViewModel: ShellViewModel by viewModels()
    private lateinit var navigator: ShellNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        val legacyPrefState = PrefMigrator.migrateIfNeeded(applicationContext)
        PrefMigrator.migrateManagedModelIfNeeded(applicationContext, legacyPrefState)
        super.onCreate(savedInstanceState)
        binding = ActivityMainShellBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBars.setup(this)
        ToolbarHeaderInsets.apply(binding.toolbarHeader)

        setSupportActionBar(binding.toolbar)
        setupStatusBanner()
        setupBottomNav()
        setupBackNavigation()

        navigator = ShellNavigator(supportFragmentManager, R.id.fragmentContainer)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                shellViewModel.uiState.collect { state ->
                    updateXposedStatusBanner(state.xposedActive)
                }
            }
        }

        val initialTab = shellViewModel.uiState.value.selectedTab
        selectTab(initialTab, fromUser = false)

        shellViewModel.refreshXposedStatus(isModuleActive())
    }

    override fun onResume() {
        super.onResume()
        shellViewModel.refreshXposedStatus(isModuleActive())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        when (shellViewModel.uiState.value.selectedTab) {
            ShellTab.CONFIG -> menuInflater.inflate(R.menu.menu_main, menu)
            ShellTab.OBSERVE -> menuInflater.inflate(R.menu.menu_observe_stub, menu)
            else -> menu.clear()
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        when (shellViewModel.uiState.value.selectedTab) {
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
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val configFragment = navigator.findFragment(ShellTab.CONFIG) as? ConfigFragment
        return configFragment?.handleOptionsItem(item) == true || super.onOptionsItemSelected(item)
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
        val currentTab = shellViewModel.uiState.value.selectedTab
        if (currentTab == tab && navigator.findFragment(tab) != null) {
            syncBottomNav(tab)
            return
        }

        if (fromUser) {
            shellViewModel.setSelectedTab(tab)
        }

        navigator.select(tab)
        syncBottomNav(tab)
        invalidateOptionsMenu()
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

    private fun setupStatusBanner() {
        StatusBanner.setOnClickListener(binding.statusBanner.root) {
            if (!XposedManagerLauncher.open(this)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.xposed_not_active)
                    .setMessage(R.string.xposed_hint)
                    .show()
            }
        }
    }

    private fun updateXposedStatusBanner(active: Boolean) {
        StatusBanner.bind(binding.statusBanner.root, active)
    }

    private fun isModuleActive(): Boolean = ModuleActivation.isModuleActive()

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (shellViewModel.uiState.value.selectedTab == ShellTab.OBSERVE) {
                        selectTab(ShellTab.CONFIG, fromUser = true)
                        return
                    }
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            },
        )
    }
}
