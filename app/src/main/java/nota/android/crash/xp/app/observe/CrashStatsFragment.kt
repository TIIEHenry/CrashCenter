package nota.android.crash.xp.app.observe

import android.os.Bundle
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
        EmptyState.bind(binding.emptyState.root, getString(R.string.stats_empty), R.drawable.ic_tab_observe)
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
        )
        binding.topExceptionsCard.visibility =
            if (stats.topExceptionClasses.isNotEmpty()) View.VISIBLE else View.GONE
        binding.topExceptionsHeader.visibility =
            if (stats.topExceptionClasses.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun <T> renderEntries(
        container: ViewGroup,
        entries: List<T>,
        labelExtractor: (T) -> String,
        countExtractor: (T) -> Int,
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        for (entry in entries) {
            val itemView = inflater.inflate(R.layout.item_stat_entry, container, false)
            itemView.findViewById<TextView>(R.id.entryLabel).text = labelExtractor(entry)
            itemView.findViewById<TextView>(R.id.entryCount).text = countExtractor(entry).toString()
            container.addView(itemView)
        }
    }

    companion object {
        const val TAG = "crash_stats"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun newInstance(): CrashStatsFragment = CrashStatsFragment()
    }
}
