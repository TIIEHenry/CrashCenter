package nota.android.crash.xp.app.observe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.databinding.FragmentLogcatBinding
import nota.android.crash.xp.app.di.ViewModelFactory

class LogcatFragment : Fragment() {

    private var _binding: FragmentLogcatBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private val viewModel: LogcatViewModel by viewModels {
        ViewModelFactory { LogcatViewModel() }
    }

    private lateinit var adapter: LogcatAdapter

    private val safLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadFromUri(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLogcatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupChips()
        EmptyState.bind(binding.emptyState.root, getString(R.string.logcat_empty), R.drawable.ic_tab_observe)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
        // Auto-launch SAF if no data loaded yet
        if (viewModel.uiState.value.entries.isEmpty() && savedInstanceState == null) {
            launchFilePicker()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── Setup ───

    private fun setupList() {
        adapter = LogcatAdapter { entry ->
            showEntryDetail(entry)
        }
        binding.recyclerView.apply {
            adapter = this@LogcatFragment.adapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        }
    }

    private fun setupChips() {
        val chipMap = mapOf(
            R.id.chipFatal to LogcatLevel.FATAL,
            R.id.chipError to LogcatLevel.ERROR,
            R.id.chipWarning to LogcatLevel.WARNING,
            R.id.chipInfo to LogcatLevel.INFO,
            R.id.chipDebug to LogcatLevel.DEBUG,
        )
        for ((chipId, level) in chipMap) {
            binding.root.findViewById<Chip>(chipId)?.setOnCheckedChangeListener { _, _ ->
                viewModel.toggleLevel(level)
            }
        }
    }

    // ─── SAF file picker ───

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/x-log", "application/octet-stream"))
        }
        safLauncher.launch(intent)
    }

    private fun loadFromUri(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val text = requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().use { it.readText() }
                }
                if (text.isNullOrBlank()) {
                    Toast.makeText(requireContext(), R.string.logcat_file_empty, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (text.length > 5 * 1024 * 1024) {
                    Toast.makeText(requireContext(), R.string.logcat_file_too_large, Toast.LENGTH_LONG).show()
                }
                viewModel.loadFromText(text)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.logcat_import_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Render ───

    private fun renderState(state: LogcatUiState) {
        binding.loadingPanel.root.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        if (state.isLoading) {
            LoadingState.bind(binding.loadingPanel.root, getString(R.string.logcat_loading))
        }

        val hasData = !state.isLoading && state.entries.isNotEmpty()
        val isEmpty = !state.isLoading && state.entries.isEmpty()

        binding.contentContainer.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.emptyState.root.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (isEmpty && !state.isLoading) {
            EmptyState.bind(
                binding.emptyState.root,
                getString(R.string.logcat_empty),
                getString(R.string.logcat_import_action),
                { launchFilePicker() },
                R.drawable.ic_tab_observe,
            )
        }

        if (hasData) {
            val displayEntries = state.displayEntries
            adapter.submitList(displayEntries)
            val countText = if (state.totalRawCount != state.entries.size) {
                resources.getQuantityString(
                    R.plurals.logcat_entry_count_filtered,
                    displayEntries.size,
                    displayEntries.size,
                    state.totalRawCount,
                )
            } else {
                resources.getQuantityString(R.plurals.logcat_entry_count, displayEntries.size, displayEntries.size)
            }
            binding.entryCount.text = countText
        }

        requireContext().showErrorToast(state.errorMessage) { viewModel.clearError() }
    }

    private fun showEntryDetail(entry: LogcatEntry) {
        CrashDetailBottomSheet.newInstanceStackTrace(
            stackTrace = entry.rawLine,
            title = "${entry.level.label}/${entry.tag}",
        ).show(parentFragmentManager, CrashDetailBottomSheet.TAG)
    }

    // ─── Options Menu ───

    fun handleOptionsItem(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_observe_import_logcat -> {
                launchFilePicker()
                true
            }
            else -> false
        }
    }

    companion object {
        const val TAG = "logcat"

        fun newInstance(): LogcatFragment = LogcatFragment()
    }
}
