package nota.android.crash.xp.app.shell

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import com.google.android.material.navigation.NavigationBarMenuView
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
    private var bottomNavClicksAttached = false
    private var isSyncingNav = false

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

        shellViewModel.uiState.observe(this) { state ->
            updateXposedStatusBanner(state.xposedActive)
        }

        showTab(shellViewModel.uiState.value?.selectedTab ?: ShellTab.CONFIG)

        shellViewModel.refreshXposedStatus(isModuleActive())
    }

    override fun onResume() {
        super.onResume()
        shellViewModel.refreshXposedStatus(isModuleActive())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        when (shellViewModel.uiState.value?.selectedTab) {
            ShellTab.CONFIG -> menuInflater.inflate(R.menu.menu_main, menu)
            ShellTab.OBSERVE -> menuInflater.inflate(R.menu.menu_observe_stub, menu)
            else -> menu.clear()
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        when (shellViewModel.uiState.value?.selectedTab) {
            ShellTab.CONFIG -> {
                (supportFragmentManager.findFragmentByTag(ConfigFragment.TAG) as? ConfigFragment)
                    ?.prepareOptionsMenu(menu)
            }
            ShellTab.OBSERVE -> {
                for (index in 0 until menu.size()) {
                    menu.getItem(index).isEnabled = false
                }
            }
            else -> Unit
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val configFragment = supportFragmentManager.findFragmentByTag(ConfigFragment.TAG) as? ConfigFragment
        return configFragment?.handleOptionsItem(item) == true || super.onOptionsItemSelected(item)
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (isSyncingNav) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.nav_config -> {
                    showTab(ShellTab.CONFIG)
                    true
                }
                R.id.nav_observe -> {
                    showTab(ShellTab.OBSERVE)
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.doOnLayout { attachBottomNavItemClicks() }
    }

    private fun attachBottomNavItemClicks() {
        if (bottomNavClicksAttached) return
        val menuView = binding.bottomNav.getChildAt(0) as? NavigationBarMenuView ?: return
        bottomNavClicksAttached = true
        for (index in 0 until menuView.childCount) {
            val itemView = menuView.getChildAt(index)
            val tab = when (itemView.id) {
                R.id.nav_config -> ShellTab.CONFIG
                R.id.nav_observe -> ShellTab.OBSERVE
                else -> continue
            }
            itemView.setOnClickListener { showTab(tab) }
        }
    }

    private fun showTab(tab: ShellTab) {
        val fm = supportFragmentManager
        val fragmentsReady = fm.findFragmentByTag(ConfigFragment.TAG) != null &&
            fm.findFragmentByTag(ObserveHostFragment.TAG) != null
        if (fragmentsReady && shellViewModel.uiState.value?.selectedTab == tab) {
            syncBottomNavSelection(tab)
            return
        }

        shellViewModel.setSelectedTab(tab)
        var config = fm.findFragmentByTag(ConfigFragment.TAG)
        var observe = fm.findFragmentByTag(ObserveHostFragment.TAG)

        fm.commitNow {
            setReorderingAllowed(true)
            if (config == null) {
                config = ConfigFragment.newInstance()
                add(R.id.fragmentContainer, config, ConfigFragment.TAG)
            }
            if (observe == null) {
                observe = ObserveHostFragment.newInstance()
                add(R.id.fragmentContainer, observe, ObserveHostFragment.TAG)
            }
            when (tab) {
                ShellTab.CONFIG -> {
                    show(config)
                    setMaxLifecycle(config, Lifecycle.State.RESUMED)
                    hide(observe)
                    setMaxLifecycle(observe, Lifecycle.State.STARTED)
                }
                ShellTab.OBSERVE -> {
                    hide(config)
                    setMaxLifecycle(config, Lifecycle.State.STARTED)
                    show(observe)
                    setMaxLifecycle(observe, Lifecycle.State.RESUMED)
                }
            }
        }

        syncBottomNavSelection(tab)
        invalidateOptionsMenu()
    }

    private fun syncBottomNavSelection(tab: ShellTab) {
        val menuItemId = when (tab) {
            ShellTab.CONFIG -> R.id.nav_config
            ShellTab.OBSERVE -> R.id.nav_observe
        }
        if (binding.bottomNav.selectedItemId != menuItemId) {
            isSyncingNav = true
            try {
                binding.bottomNav.selectedItemId = menuItemId
            } finally {
                isSyncingNav = false
            }
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
                    if (shellViewModel.uiState.value?.selectedTab == ShellTab.OBSERVE) {
                        showTab(ShellTab.CONFIG)
                        return
                    }
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            },
        )
    }
}
