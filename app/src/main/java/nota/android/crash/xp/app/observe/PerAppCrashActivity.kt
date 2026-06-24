package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.SystemBars
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.common.ui.RecyclerViewListSetup
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.config.PackageInfoLoader
import nota.android.crash.xp.app.data.PerAppStats
import nota.android.crash.xp.app.databinding.ActivityPerAppCrashBinding
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.perAppCrashViewModelFactory

class PerAppCrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerAppCrashBinding
    private var packageName: String? = null
    private var exceptionClass: String? = null
    private lateinit var adapter: CrashHistoryPagingAdapter
    private var lastHistoryCleared = 0
    private lateinit var menuActions: PerAppCrashMenuActions

    private val viewModel: PerAppCrashViewModel by viewModels {
        ServiceLocator.perAppCrashViewModelFactory(this, packageName, exceptionClass)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        exceptionClass = intent.getStringExtra(EXTRA_EXCEPTION_CLASS)
        if (packageName == null && exceptionClass == null) {
            finish()
            return
        }

        binding = ActivityPerAppCrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBars.setup(this)
        SystemBars.applyToolbarHeaderInsets(binding.toolbarHeader)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadAppHeader()
        setupList()
        setupFilter()
        menuActions = PerAppCrashMenuActions(
            activity = this,
            packageName = packageName,
            getAppLabel = { binding.tvLabel.text.toString() },
            onClearConfirmed = {
                viewModel.clearRecords()
                Toast.makeText(this, R.string.per_app_clear_success, Toast.LENGTH_SHORT).show()
            },
        )
        setupMenu()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pagingData.collectLatest { adapter.submitData(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderSummary(it) }
            }
        }
        viewModel.loadSummary()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.refresh()
        }
    }

    private fun loadAppHeader() {
        val pkg = packageName
        if (pkg == null) {
            binding.tvPackageName.visibility = View.GONE
            binding.tvLabel.text = exceptionClass ?: ""
            supportActionBar?.title = exceptionClass ?: ""
            binding.tvUninstalled.visibility = View.GONE
            binding.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            return
        }
        binding.tvPackageName.text = pkg
        val appInfo = PackageInfoLoader.loadAppInfo(packageManager, pkg)?.second
        if (appInfo == null) {
            supportActionBar?.title = pkg
            binding.tvLabel.text = pkg
            binding.tvUninstalled.visibility = View.VISIBLE
            binding.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            return
        }
        binding.tvUninstalled.visibility = View.GONE
        binding.ivIcon.setImageDrawable(try {
            appInfo.loadIcon(packageManager)
        } catch (_: Exception) {
            packageManager.getDrawable("android", android.R.drawable.sym_def_app_icon, null)
        })
        val label = PackageInfoLoader.loadLabel(packageManager, appInfo)
        binding.tvLabel.text = label
        supportActionBar?.title = label
    }

    private fun setupList() {
        adapter = CrashHistoryPagingAdapter(
            onItemClick = { event ->
                CrashDetailBottomSheet.newInstance(event.id)
                    .show(supportFragmentManager, CrashDetailBottomSheet.TAG)
            },
            displayMode = CrashHistoryDisplayMode.PER_APP,
        )
        binding.recyclerView.apply {
            adapter = this@PerAppCrashActivity.adapter
            RecyclerViewListSetup.apply(this, this@PerAppCrashActivity)
        }
        EmptyState.bind(binding.emptyState.root, getString(R.string.per_app_empty), R.drawable.ic_tab_observe)
        adapter.addLoadStateListener { loadStates ->
            val isLoading = loadStates.refresh is LoadState.Loading
            val isEmpty = loadStates.refresh is LoadState.NotLoading && adapter.itemCount == 0

            binding.loadingPanel.root.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                LoadingState.bind(binding.loadingPanel.root, getString(R.string.per_app_loading))
            }

            val hasEvents = !isLoading && adapter.itemCount > 0
            binding.recyclerView.visibility = if (hasEvents) View.VISIBLE else View.GONE
            binding.eventCount.visibility = if (hasEvents) View.VISIBLE else View.GONE

            if (hasEvents) {
                val count = adapter.itemCount
                binding.eventCount.text = resources.getQuantityString(R.plurals.per_app_count, count, count)
            }

            val empty = !isLoading && isEmpty
            binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
            if (empty) {
                EmptyState.bind(binding.emptyState.root, getString(R.string.per_app_empty), R.drawable.ic_tab_observe)
            }
        }
    }

    private fun setupFilter() {
        binding.etFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setFilterQuery(s?.toString().orEmpty())
            }
        })
    }

    private fun setupMenu() {
        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                    menuInflater.inflate(R.menu.menu_per_app_crash, menu)
                }

                override fun onPrepareMenu(menu: android.view.Menu) {
                    menuActions.prepareMenu(menu)
                }

                override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                    return menuActions.handleItem(menuItem)
                }
            },
            this,
            Lifecycle.State.RESUMED,
        )
    }

    private fun renderSummary(state: PerAppCrashUiState) {
        val summary = state.summary
        if (summary != null) {
            renderSummaryStats(summary)
        }
        if (state.historyCleared != lastHistoryCleared) {
            lastHistoryCleared = state.historyCleared
            adapter.refresh()
            viewModel.loadSummary()
        }
        showErrorToast(state.errorMessage) { viewModel.clearError() }
    }

    private fun renderSummaryStats(summary: PerAppStats) {
        val relativeRecent = if (summary.mostRecentTimestampMs > 0) {
            DateUtils.getRelativeTimeSpanString(
                summary.mostRecentTimestampMs,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            )
        } else {
            getString(R.string.stats_none)
        }
        binding.tvSummary.text = getString(
            R.string.per_app_summary_format,
            summary.totalCount,
            relativeRecent,
        )

        val topClass = summary.topExceptionClass
        if (topClass != null && summary.topExceptionCount > 0) {
            binding.tvTopException.visibility = View.VISIBLE
            binding.tvTopException.text = getString(
                R.string.per_app_top_exception_format,
                topClass,
                summary.topExceptionCount,
            )
        } else {
            binding.tvTopException.visibility = View.GONE
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_EXCEPTION_CLASS = "extra_exception_class"
    }
}
