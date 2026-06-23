package nota.android.crash.xp.app.shell

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

import nota.android.crash.xp.PrefMigrator
import nota.android.crash.xp.XposedManagerLauncher
import nota.android.crash.xp.app.ModuleActivation
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.SystemBars
import nota.android.crash.xp.app.common.ui.StatusBanner
import nota.android.crash.xp.app.databinding.ActivityMainShellBinding
import nota.android.crash.xp.app.di.shellViewModelFactory

class MainShellActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainShellBinding
    private val shellViewModel: ShellViewModel by viewModels {
        ServiceLocator.shellViewModelFactory(this)
    }
    private lateinit var navigator: ShellNavigator
    private lateinit var tabController: ShellTabController

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = ServiceLocator.prefs(applicationContext)
        val legacyPrefState = PrefMigrator.migrateIfNeeded(
            applicationContext, prefs, ServiceLocator.rootAccessClient(applicationContext)
        )
        PrefMigrator.migrateManagedModelIfNeeded(applicationContext, prefs, legacyPrefState)
        PrefMigrator.migrateObserveInterceptSplitIfNeeded(prefs)
        super.onCreate(savedInstanceState)
        binding = ActivityMainShellBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBars.setup(this)
        SystemBars.applyToolbarHeaderInsets(binding.toolbarHeader)

        setSupportActionBar(binding.toolbar)
        setupStatusBanner()

        navigator = ShellNavigator(supportFragmentManager, R.id.fragmentContainer)
        tabController = ShellTabController(this, binding, shellViewModel, navigator)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                shellViewModel.uiState.collect { state ->
                    updateXposedStatusBanner(state)
                }
            }
        }

        val initialTab = shellViewModel.uiState.value.selectedTab
        tabController.onCreate(initialTab)

        shellViewModel.refreshXposedStatus(isModuleActive())
        shellViewModel.refreshRootStatus(applicationContext, ServiceLocator.rootAccessClient(applicationContext))
    }

    override fun onResume() {
        super.onResume()
        tabController.onResume()
        shellViewModel.refreshRootStatus(applicationContext, ServiceLocator.rootAccessClient(applicationContext))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return tabController.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        tabController.onPrepareOptionsMenu(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return tabController.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

    fun requestShellTab(tab: ShellTab) {
        tabController.selectShellTab(tab)
    }

    private fun setupStatusBanner() {
        StatusBanner.setOnClickListener(binding.statusBanner.root) {
            if (!XposedManagerLauncher.open(this)) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.xposed_not_active)
                    .setMessage(R.string.xposed_hint)
                    .show()
            }
        }
    }

    private fun updateXposedStatusBanner(state: ShellUiState) {
        StatusBanner.bind(
            root = binding.statusBanner.root,
            active = state.xposedActive,
            rootAvailability = state.rootAvailability,
            activeBackendCount = state.activeBackendCount,
            totalBackendCount = state.totalBackendCount,
        )
    }

    private fun isModuleActive(): Boolean = ModuleActivation.isModuleActive()
}
