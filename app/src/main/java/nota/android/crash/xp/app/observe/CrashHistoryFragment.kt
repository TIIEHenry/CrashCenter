package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.crashHistoryViewModelFactory
import nota.android.crash.xp.app.databinding.FragmentCrashHistoryBinding

class CrashHistoryFragment : Fragment() {

    private var _binding: FragmentCrashHistoryBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private val viewModel: CrashHistoryViewModel by viewModels {
        ServiceLocator.crashHistoryViewModelFactory(requireContext())
    }

    private lateinit var adapter: CrashHistoryPagingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCrashHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        EmptyState.bind(binding.emptyState.root, getString(R.string.crash_history_empty), R.drawable.ic_tab_observe)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pagingData.collectLatest { adapter.submitData(it) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
        viewModel.loadEvents()
    }

    override fun onResume() {
        super.onResume()
        adapter.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupList() {
        adapter = CrashHistoryPagingAdapter { event ->
            openDetail(event.id)
        }
        binding.recyclerView.apply {
            adapter = this@CrashHistoryFragment.adapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        }
        adapter.addLoadStateListener { loadStates ->
            val isLoading = loadStates.refresh is LoadState.Loading
            val isEmpty = loadStates.refresh is LoadState.NotLoading && adapter.itemCount == 0
            val hasError = loadStates.refresh is LoadState.Error

            binding.loadingPanel.root.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                LoadingState.bind(binding.loadingPanel.root, getString(R.string.crash_history_loading))
            }

            val hasEvents = !isLoading && adapter.itemCount > 0
            binding.recyclerView.visibility = if (hasEvents) View.VISIBLE else View.GONE
            binding.eventCount.visibility = if (hasEvents) View.VISIBLE else View.GONE

            if (hasEvents) {
                val count = adapter.itemCount
                binding.eventCount.text = resources.getQuantityString(R.plurals.crash_history_count, count, count)
            }

            val empty = !isLoading && isEmpty
            binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
            if (empty) {
                EmptyState.bind(binding.emptyState.root, getString(R.string.crash_history_empty), R.drawable.ic_tab_observe)
            }
        }
    }

    private fun openDetail(crashId: String) {
        CrashDetailBottomSheet.newInstance(crashId)
            .show(parentFragmentManager, CrashDetailBottomSheet.TAG)
    }

    private fun renderState(state: CrashHistoryUiState) {
        // Event count is now shown via adapter load state listener
        // but we also update from repository count for accuracy
        if (state.eventCount > 0) {
            binding.eventCount.text = resources.getQuantityString(R.plurals.crash_history_count, state.eventCount, state.eventCount)
        }
        requireContext().showErrorToast(state.errorMessage) { viewModel.clearError() }
    }

    companion object {
        const val TAG = "crash_history"

        fun newInstance(): CrashHistoryFragment = CrashHistoryFragment()
    }
}
