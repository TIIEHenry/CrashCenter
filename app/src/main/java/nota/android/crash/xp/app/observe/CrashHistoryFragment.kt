package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.data.FileCrashLogRepository
import nota.android.crash.xp.app.databinding.FragmentCrashHistoryBinding

class CrashHistoryFragment : Fragment() {

    private var _binding: FragmentCrashHistoryBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private val viewModel: CrashHistoryViewModel by viewModels {
        CrashHistoryViewModel.Factory(FileCrashLogRepository(requireContext()))
    }

    private lateinit var adapter: CrashHistoryAdapter

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
                viewModel.uiState.collect { renderState(it) }
            }
        }
        viewModel.loadEvents(forceReload = savedInstanceState == null)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEvents(forceReload = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupList() {
        adapter = CrashHistoryAdapter()
        adapter.onItemClick { _, event, _ ->
            openDetail(event.id)
        }
        binding.recyclerView.apply {
            adapter = this@CrashHistoryFragment.adapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        }
    }

    private fun openDetail(crashId: String) {
        CrashDetailBottomSheet.newInstance(crashId)
            .show(parentFragmentManager, CrashDetailBottomSheet.TAG)
    }

    private fun renderState(state: CrashHistoryUiState) {
        binding.loadingPanel.root.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        if (state.isLoading) {
            LoadingState.bind(binding.loadingPanel.root, getString(R.string.crash_history_loading))
        }

        val hasEvents = !state.isLoading && state.events.isNotEmpty()
        binding.recyclerView.visibility = if (hasEvents) View.VISIBLE else View.GONE
        binding.eventCount.visibility = if (hasEvents) View.VISIBLE else View.GONE

        if (hasEvents) {
            binding.eventCount.text = resources.getQuantityString(R.plurals.crash_history_count, state.eventCount, state.eventCount)
            adapter.setData(state.events)
        }

        val empty = !state.isLoading && state.events.isEmpty()
        binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
        if (empty) {
            EmptyState.bind(binding.emptyState.root, getString(R.string.crash_history_empty), R.drawable.ic_tab_observe)
        }
    }

    companion object {
        const val TAG = "crash_history"
        const val EXTRA_CRASH_ID = "crash_id"

        fun newInstance(): CrashHistoryFragment = CrashHistoryFragment()
    }
}
