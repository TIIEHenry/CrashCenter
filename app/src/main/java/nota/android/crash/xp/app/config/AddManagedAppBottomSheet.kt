package nota.android.crash.xp.app.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.DenseSearchField
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.common.ui.configureBottomSheetAppearance
import nota.android.crash.xp.app.databinding.BottomSheetAddManagedAppBinding

class AddManagedAppBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddManagedAppBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private val viewModel: AddManagedAppViewModel by viewModels {
        AddManagedAppViewModel.Factory(AppRepository(requireContext()))
    }

    private lateinit var adapter: PickableAppAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetAddManagedAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupToolbar()
        setupSearch()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        configureBottomSheetAppearance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupToolbar() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnDone.setOnClickListener { commitSelection() }
    }

    private fun setupSearch() {
        DenseSearchField.setOnQueryChangeListener(binding.searchField.root) { newQuery ->
            viewModel.setQuery(newQuery)
        }
    }

    private fun setupList() {
        adapter = PickableAppAdapter()
        adapter.onItemClick { app ->
            adapter.toggleSelection(app)
        }
        binding.recyclerPickable.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPickable.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AddManagedAppUiState.Loading -> {
                            binding.loadingPanel.root.visibility = View.VISIBLE
                            LoadingState.bind(binding.loadingPanel.root)
                            binding.recyclerPickable.visibility = View.GONE
                            binding.emptyState.root.visibility = View.GONE
                        }
                        is AddManagedAppUiState.Success -> {
                            if (_binding == null) return@collect
                            binding.loadingPanel.root.visibility = View.GONE
                            adapter.submitList(state.apps)

                            val empty = state.apps.isEmpty()
                            if (empty) {
                                EmptyState.bind(
                                    binding.emptyState.root,
                                    getString(R.string.add_managed_picker_empty),
                                    R.drawable.ic_add,
                                )
                            }
                            binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
                            binding.recyclerPickable.visibility = if (!empty) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun commitSelection() {
        val selected = adapter.selectedPackages()
        if (selected.isEmpty()) {
            dismiss()
            return
        }
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putStringArrayList(ARG_PACKAGES, ArrayList(selected))
            },
        )
        Toast.makeText(
            requireContext(),
            resources.getQuantityString(R.plurals.add_managed_success_count, selected.size, selected.size),
            Toast.LENGTH_SHORT,
        ).show()
        dismiss()
    }

    companion object {
        const val TAG = "add_managed_app"
        const val REQUEST_KEY = "add_managed_app_result"
        const val ARG_PACKAGES = "packages"

        fun newInstance(): AddManagedAppBottomSheet = AddManagedAppBottomSheet()
    }
}
