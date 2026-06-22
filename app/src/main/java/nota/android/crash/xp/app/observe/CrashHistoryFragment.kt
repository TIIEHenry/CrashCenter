package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.data.CrashFilter
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
        if (state.eventCount > 0) {
            binding.eventCount.text = resources.getQuantityString(R.plurals.crash_history_count, state.eventCount, state.eventCount)
        }
        requireContext().showErrorToast(state.errorMessage) { viewModel.clearError() }
    }

    // ─── Options Menu ───

    fun prepareOptionsMenu(menu: Menu) {
        val filterItem = menu.findItem(R.id.item_observe_filter)
        val hasFilter = viewModel.uiState.value.activeFilter != null
        filterItem?.setTitle(
            if (hasFilter) R.string.observe_menu_filter_clear
            else R.string.observe_menu_filter
        )
    }

    fun handleOptionsItem(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_observe_filter -> {
                val activeFilter = viewModel.uiState.value.activeFilter
                if (activeFilter != null) {
                    viewModel.setFilter(CrashFilter())
                } else {
                    showFilterDialog()
                }
                true
            }
            R.id.item_observe_export -> {
                Toast.makeText(requireContext(), "TODO: Export", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.item_observe_stats -> {
                Toast.makeText(requireContext(), "TODO: Stats", Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }

    private fun showFilterDialog() {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.observe_filter_package_hint)
            setSingleLine()
            setPadding(64, 32, 64, 16)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.observe_filter_title)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    viewModel.setFilter(CrashFilter(packageName = input))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val TAG = "crash_history"

        fun newInstance(): CrashHistoryFragment = CrashHistoryFragment()
    }
}
