package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.SystemBars
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.config.PackageInfoLoader
import nota.android.crash.xp.app.data.PerAppStats
import nota.android.crash.xp.app.databinding.ActivityPerAppCrashBinding
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.perAppCrashViewModelFactory

class PerAppCrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerAppCrashBinding
    private lateinit var packageName: String
    private lateinit var adapter: CrashHistoryPagingAdapter

    private val viewModel: PerAppCrashViewModel by viewModels {
        ServiceLocator.perAppCrashViewModelFactory(this, packageName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            ?: run {
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
        binding.tvPackageName.text = packageName
        val appInfo = PackageInfoLoader.loadAppInfo(packageManager, packageName)?.second
        if (appInfo == null) {
            binding.toolbar.title = packageName
            binding.tvLabel.text = packageName
            binding.tvUninstalled.visibility = View.VISIBLE
            binding.ivIcon.setImageResource(R.mipmap.ic_launcher)
            return
        }
        binding.tvUninstalled.visibility = View.GONE
        binding.ivIcon.setImageDrawable(appInfo.loadIcon(packageManager))
        val label = PackageInfoLoader.loadLabel(packageManager, appInfo)
        binding.tvLabel.text = label
        binding.toolbar.title = label
    }

    private fun setupList() {
        adapter = CrashHistoryPagingAdapter { event ->
            CrashDetailBottomSheet.newInstance(event.id)
                .show(supportFragmentManager, CrashDetailBottomSheet.TAG)
        }
        binding.recyclerView.apply {
            adapter = this@PerAppCrashActivity.adapter
            layoutManager = LinearLayoutManager(this@PerAppCrashActivity, RecyclerView.VERTICAL, false)
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

    private fun renderSummary(state: PerAppCrashUiState) {
        val summary = state.summary
        if (summary != null) {
            renderSummaryStats(summary)
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
    }
}
