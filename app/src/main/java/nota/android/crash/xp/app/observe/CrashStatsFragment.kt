package nota.android.crash.xp.app.observe

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.data.CategoryCount
import nota.android.crash.xp.app.data.ClusterCount
import nota.android.crash.xp.app.data.CrashStats
import nota.android.crash.xp.app.data.ExceptionCount
import nota.android.crash.xp.app.data.PackageCount
import nota.android.crash.xp.app.databinding.FragmentCrashStatsBinding
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.crashStatsViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashStatsFragment : Fragment() {

    private var _binding: FragmentCrashStatsBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private val viewModel: CrashStatsViewModel by viewModels {
        ServiceLocator.crashStatsViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCrashStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EmptyState.bind(
            binding.emptyState.root,
            getString(R.string.stats_empty),
            getString(R.string.stats_empty_action),
            { (parentFragment as? ObserveHostFragment)?.selectSubTab(0) },
            R.drawable.ic_tab_observe,
        )
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
        viewModel.loadStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderState(state: CrashStatsUiState) {
        binding.loadingPanel.root.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        if (state.isLoading) {
            LoadingState.bind(binding.loadingPanel.root, getString(R.string.stats_loading))
        }

        val stats = state.stats
        val hasData = !state.isLoading && stats != null && stats.totalCount > 0
        val isEmpty = !state.isLoading && (stats == null || stats.totalCount == 0)

        binding.scrollView.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.emptyState.root.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (isEmpty) {
            EmptyState.bind(
                binding.emptyState.root,
                getString(R.string.stats_empty),
                getString(R.string.stats_empty_action),
                { (parentFragment as? ObserveHostFragment)?.selectSubTab(0) },
                R.drawable.ic_tab_observe,
            )
        }

        if (hasData) {
            renderStats(stats)
        }

        requireContext().showErrorToast(state.errorMessage) { viewModel.clearError() }
    }

    private fun renderStats(stats: CrashStats) {
        // Summary cards
        binding.statTotalCount.text = stats.totalCount.toString()
        binding.statUniquePackages.text = stats.uniquePackageCount.toString()
        binding.statMostRecent.text = if (stats.mostRecentTimestampMs > 0) {
            DATE_FORMAT.format(Date(stats.mostRecentTimestampMs))
        } else {
            getString(R.string.stats_none)
        }

        // Top packages
        renderEntries(
            container = binding.topPackagesContainer,
            entries = stats.topPackages,
            labelExtractor = PackageCount::packageName,
            countExtractor = PackageCount::count,
            onClick = { entry -> openPerAppCrash(entry.packageName) },
        )
        binding.topPackagesCard.visibility =
            if (stats.topPackages.isNotEmpty()) View.VISIBLE else View.GONE
        binding.topPackagesHeader.visibility =
            if (stats.topPackages.isNotEmpty()) View.VISIBLE else View.GONE

        // Top exceptions
        renderEntries(
            container = binding.topExceptionsContainer,
            entries = stats.topExceptionClasses,
            labelExtractor = ExceptionCount::exceptionClass,
            countExtractor = ExceptionCount::count,
            onClick = { entry -> openPerAppCrashForException(entry.exceptionClass) },
        )
        binding.topExceptionsCard.visibility =
            if (stats.topExceptionClasses.isNotEmpty()) View.VISIBLE else View.GONE
        binding.topExceptionsHeader.visibility =
            if (stats.topExceptionClasses.isNotEmpty()) View.VISIBLE else View.GONE

        renderEntries(
            container = binding.topCategoriesContainer,
            entries = stats.topCategories,
            labelExtractor = CategoryCount::category,
            countExtractor = CategoryCount::count,
            onClick = { entry -> openPerAppCrashForException(entry.category) },
        )
        binding.topCategoriesCard.visibility =
            if (stats.topCategories.isNotEmpty()) View.VISIBLE else View.GONE
        binding.topCategoriesHeader.visibility =
            if (stats.topCategories.isNotEmpty()) View.VISIBLE else View.GONE

        renderEntries(
            container = binding.topClustersContainer,
            entries = stats.topClusters,
            labelExtractor = ClusterCount::label,
            countExtractor = ClusterCount::count,
        )
        binding.topClustersCard.visibility =
            if (stats.topClusters.isNotEmpty()) View.VISIBLE else View.GONE
        binding.topClustersHeader.visibility =
            if (stats.topClusters.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun <T> renderEntries(
        container: ViewGroup,
        entries: List<T>,
        labelExtractor: (T) -> String,
        countExtractor: (T) -> Int,
        onClick: ((T) -> Unit)? = null,
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val selectableBackground = if (onClick != null) {
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
            typedValue.resourceId
        } else {
            0
        }
        for (entry in entries) {
            val itemView = inflater.inflate(R.layout.item_stat_entry, container, false)
            itemView.findViewById<TextView>(R.id.entryLabel).text = labelExtractor(entry)
            itemView.findViewById<TextView>(R.id.entryCount).text = countExtractor(entry).toString()
            if (onClick != null) {
                itemView.setBackgroundResource(selectableBackground)
                itemView.setOnClickListener { onClick(entry) }
            }
            container.addView(itemView)
        }
    }

    private fun openPerAppCrash(packageName: String) {
        startActivity(
            Intent(requireContext(), PerAppCrashActivity::class.java).apply {
                putExtra(PerAppCrashActivity.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    private fun openPerAppCrashForException(exceptionClass: String) {
        startActivity(
            Intent(requireContext(), PerAppCrashActivity::class.java).apply {
                putExtra(PerAppCrashActivity.EXTRA_EXCEPTION_CLASS, exceptionClass)
            },
        )
    }

    companion object {
        const val TAG = "crash_stats"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun newInstance(): CrashStatsFragment = CrashStatsFragment()
    }
}
