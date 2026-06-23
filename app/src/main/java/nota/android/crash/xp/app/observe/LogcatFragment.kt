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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.root.RootLogcatReader
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
        setupModeChips()
        setupBufferChips()
        setupLevelChips()
        EmptyState.bind(binding.emptyState.root, getString(R.string.logcat_empty), R.drawable.ic_tab_observe)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
        // Auto-load from root if available, otherwise prompt file import
        if (viewModel.uiState.value.entries.isEmpty() && savedInstanceState == null) {
            checkRootAndLoad()
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

    private fun setupModeChips() {
        val rootAvailable = RootLogcatReader.isAvailable(requireContext())
        binding.chipModeRoot.isEnabled = rootAvailable
        if (!rootAvailable) {
            binding.chipModeRoot.isChecked = false
            binding.chipModeFile.isChecked = true
        }

        binding.chipGroupMode.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            when (checkedId) {
                R.id.chipModeRoot -> {
                    binding.bufferChipRow.visibility = View.VISIBLE
                    val buffer = resolveSelectedBuffer()
                    viewModel.loadFromRoot(requireContext(), buffer)
                }
                R.id.chipModeFile -> {
                    binding.bufferChipRow.visibility = View.GONE
                    if (viewModel.uiState.value.entries.isEmpty()) {
                        launchFilePicker()
                    }
                }
            }
        }

        // Initial visibility
        val initialMode = viewModel.uiState.value.sourceMode
        if (initialMode == SourceMode.ROOT) {
            binding.chipModeRoot.isChecked = true
            binding.bufferChipRow.visibility = View.VISIBLE
        } else if (initialMode == SourceMode.FILE) {
            binding.chipModeFile.isChecked = true
            binding.bufferChipRow.visibility = View.GONE
        } else if (!rootAvailable) {
            binding.bufferChipRow.visibility = View.GONE
        }
    }

    private fun setupBufferChips() {
        val bufferChipMap = mapOf(
            R.id.chipBufferMain to LogcatBuffer.MAIN,
            R.id.chipBufferSystem to LogcatBuffer.SYSTEM,
            R.id.chipBufferCrash to LogcatBuffer.CRASH,
            R.id.chipBufferEvents to LogcatBuffer.EVENTS,
            R.id.chipBufferRadio to LogcatBuffer.RADIO,
        )
        // Sync checked chip with current active buffer
        val activeBuffer = viewModel.uiState.value.activeBuffer
        for ((chipId, buffer) in bufferChipMap) {
            binding.root.findViewById<Chip>(chipId)?.let { chip ->
                if (buffer == activeBuffer) chip.isChecked = true
            }
        }

        binding.chipGroupBuffer.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val buffer = bufferChipMap[checkedId] ?: return@setOnCheckedStateChangeListener
            viewModel.switchBuffer(requireContext(), buffer)
        }
    }

    private fun setupLevelChips() {
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

    private fun resolveSelectedBuffer(): LogcatBuffer {
        val bufferChipMap = mapOf(
            R.id.chipBufferMain to LogcatBuffer.MAIN,
            R.id.chipBufferSystem to LogcatBuffer.SYSTEM,
            R.id.chipBufferCrash to LogcatBuffer.CRASH,
            R.id.chipBufferEvents to LogcatBuffer.EVENTS,
            R.id.chipBufferRadio to LogcatBuffer.RADIO,
        )
        for ((chipId, buffer) in bufferChipMap) {
            if (binding.root.findViewById<Chip>(chipId)?.isChecked == true) return buffer
        }
        return LogcatBuffer.MAIN
    }

    private fun checkRootAndLoad() {
        viewLifecycleOwner.lifecycleScope.launch {
            val available = withContext(Dispatchers.IO) {
                RootLogcatReader.isAvailable(requireContext())
            }
            if (available) {
                binding.chipModeRoot.isChecked = true
                binding.bufferChipRow.visibility = View.VISIBLE
                viewModel.loadFromRoot(requireContext(), resolveSelectedBuffer())
            } else {
                binding.chipModeRoot.isEnabled = false
                binding.chipModeFile.isChecked = true
                binding.bufferChipRow.visibility = View.GONE
                launchFilePicker()
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

        // Sync mode chips (avoid triggering listener loops)
        val isRootMode = state.sourceMode == SourceMode.ROOT
        if (isRootMode && !binding.chipModeRoot.isChecked) {
            binding.chipModeRoot.isChecked = true
        } else if (state.sourceMode == SourceMode.FILE && !binding.chipModeFile.isChecked) {
            binding.chipModeFile.isChecked = true
        }
        binding.bufferChipRow.visibility = if (isRootMode) View.VISIBLE else View.GONE

        // Sync active buffer chip
        if (isRootMode) {
            syncBufferChip(state.activeBuffer)
        }

        val hasData = !state.isLoading && state.entries.isNotEmpty()
        val isEmpty = !state.isLoading && state.entries.isEmpty()

        binding.contentContainer.visibility = if (hasData || isRootMode) View.VISIBLE else View.GONE
        binding.emptyState.root.visibility = if (isEmpty && !isRootMode) View.VISIBLE else View.GONE

        if (isEmpty && !state.isLoading && !isRootMode) {
            EmptyState.bind(
                binding.emptyState.root,
                getString(R.string.logcat_empty),
                getString(R.string.logcat_import_action),
                { launchFilePicker() },
                R.drawable.ic_tab_observe,
            )
        } else if (isEmpty && !state.isLoading && isRootMode) {
            binding.contentContainer.visibility = View.VISIBLE
            binding.entryCount.text = getString(R.string.logcat_root_unavailable)
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

    private fun syncBufferChip(buffer: LogcatBuffer) {
        val chipId = when (buffer) {
            LogcatBuffer.MAIN -> R.id.chipBufferMain
            LogcatBuffer.SYSTEM -> R.id.chipBufferSystem
            LogcatBuffer.CRASH -> R.id.chipBufferCrash
            LogcatBuffer.EVENTS -> R.id.chipBufferEvents
            LogcatBuffer.RADIO -> R.id.chipBufferRadio
        }
        val chip = binding.root.findViewById<Chip>(chipId)
        if (chip != null && !chip.isChecked) {
            chip.isChecked = true
        }
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
