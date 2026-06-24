package nota.android.crash.xp.app.observe

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.root.RootAvailability
import nota.android.crash.root.RootLogcatReader
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.DenseSearchField
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.common.ui.RecyclerViewListSetup
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
    private var rootDetected = false
    private var lastRootLoadFailedShown = false
    private var rootProbeJob: Job? = null
    private var isProgrammaticBufferChange = false
    private var pendingFileImport = false

    private val safLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadFromUri(uri)
            }
        } else {
            pendingFileImport = false
            renderState(viewModel.uiState.value)
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
        setupCrashFilterChip()
        setupLevelChips()
        setupSearchField()
        EmptyState.bind(binding.emptyState.root, getString(R.string.logcat_empty), R.drawable.ic_tab_observe)
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
            RecyclerViewListSetup.apply(this, requireContext())
        }
        adapter.addLoadStateListener { loadStates ->
            val isLoading = viewModel.uiState.value.isLoading ||
                loadStates.refresh is LoadState.Loading
            val isEmpty = !isLoading &&
                loadStates.refresh is LoadState.NotLoading &&
                adapter.itemCount == 0
            val hasEntries = !isLoading && adapter.itemCount > 0

            binding.recyclerView.visibility = if (hasEntries) View.VISIBLE else View.GONE
            binding.entryCount.visibility = if (hasEntries || (isEmpty && viewModel.uiState.value.sourceMode == SourceMode.ROOT)) {
                View.VISIBLE
            } else {
                View.GONE
            }

            val hasSourceData = viewModel.uiState.value.entries.isNotEmpty()
            if (isEmpty && !viewModel.uiState.value.isLoading) {
                val isRootMode = viewModel.uiState.value.sourceMode == SourceMode.ROOT
                binding.emptyState.root.visibility = if (!isRootMode && !pendingFileImport && !hasSourceData) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            } else {
                binding.emptyState.root.visibility = View.GONE
            }
        }
    }

    private var isProgrammaticModeChange = false
    private var isProgrammaticCrashFilterChange = false
    private var isProgrammaticLevelChange = false

    private fun crashFilterEnabled(): Boolean = binding.chipCrashOnly.isChecked

    private fun setupModeChips() {
        binding.chipGroupMode.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isProgrammaticModeChange) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            when (checkedId) {
                R.id.chipModeRoot -> refreshRootLoad()
                R.id.chipModeFile -> {
                    binding.bufferChipRow.visibility = View.GONE
                    beginFileImport()
                }
            }
        }

        val initialMode = viewModel.uiState.value.sourceMode
        isProgrammaticModeChange = true
        if (initialMode == SourceMode.ROOT) {
            binding.chipModeRoot.isChecked = true
            binding.bufferChipRow.visibility = View.VISIBLE
        } else if (initialMode == SourceMode.FILE) {
            binding.chipModeFile.isChecked = true
            binding.bufferChipRow.visibility = View.GONE
        }
        isProgrammaticModeChange = false
    }

    private fun setFileModeProgrammatic() {
        isProgrammaticModeChange = true
        binding.chipModeFile.isChecked = true
        binding.bufferChipRow.visibility = View.GONE
        isProgrammaticModeChange = false
    }

    private fun setRootModeProgrammatic() {
        isProgrammaticModeChange = true
        binding.chipModeRoot.isChecked = true
        binding.bufferChipRow.visibility = View.VISIBLE
        isProgrammaticModeChange = false
    }

    private fun setupBufferChips() {
        val bufferChipMap = mapOf(
            R.id.chipBufferMain to LogcatBuffer.MAIN,
            R.id.chipBufferSystem to LogcatBuffer.SYSTEM,
            R.id.chipBufferCrash to LogcatBuffer.CRASH,
            R.id.chipBufferEvents to LogcatBuffer.EVENTS,
            R.id.chipBufferRadio to LogcatBuffer.RADIO,
        )
        val activeBuffer = viewModel.uiState.value.activeBuffer
        for ((chipId, buffer) in bufferChipMap) {
            binding.root.findViewById<Chip>(chipId)?.let { chip ->
                if (buffer == activeBuffer) chip.isChecked = true
            }
        }

        binding.chipGroupBuffer.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isProgrammaticBufferChange) return@setOnCheckedStateChangeListener
            if (viewModel.uiState.value.sourceMode != SourceMode.ROOT) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val buffer = bufferChipMap[checkedId] ?: return@setOnCheckedStateChangeListener
            viewModel.switchBuffer(requireContext(), buffer)
        }
    }

    private fun setupCrashFilterChip() {
        val prefs = requireContext().getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        val defaultOn = prefs.getBoolean(PrefManager.PREF_LOGCAT_CRASH_FILTER_DEFAULT, true)
        isProgrammaticCrashFilterChange = true
        binding.chipCrashOnly.isChecked = viewModel.uiState.value.isFiltered || defaultOn
        isProgrammaticCrashFilterChange = false
        if (viewModel.uiState.value.allEntries.isEmpty()) {
            viewModel.setCrashFilter(binding.chipCrashOnly.isChecked)
        }
        binding.chipCrashOnly.setOnCheckedChangeListener { _, checked ->
            if (isProgrammaticCrashFilterChange) return@setOnCheckedChangeListener
            prefs.edit { putBoolean(PrefManager.PREF_LOGCAT_CRASH_FILTER_DEFAULT, checked) }
            viewModel.setCrashFilter(checked)
        }
    }

    private fun setupSearchField() {
        DenseSearchField.setHint(binding.searchField.root, getString(R.string.logcat_search_hint))
        DenseSearchField.setOnQueryChangeListener(binding.searchField.root, viewModel::setSearchQuery)
    }

    private fun setupLevelChips() {
        syncLevelChips(viewModel.uiState.value.activeLevels)
        for ((chipId, level) in LEVEL_CHIP_MAP) {
            binding.root.findViewById<Chip>(chipId)?.setOnCheckedChangeListener { _, checked ->
                if (isProgrammaticLevelChange) return@setOnCheckedChangeListener
                viewModel.setLevelVisible(level, checked)
            }
        }
    }

    private fun syncLevelChips(activeLevels: Set<LogcatLevel>) {
        isProgrammaticLevelChange = true
        for ((chipId, level) in LEVEL_CHIP_MAP) {
            binding.root.findViewById<Chip>(chipId)?.isChecked = level in activeLevels
        }
        isProgrammaticLevelChange = false
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

    private fun requestRootThen(onProceed: () -> Unit) {
        val prefs = requireContext().getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PrefManager.PREF_ROOT_DIALOG_DISMISSED, false)) {
            onProceed()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val rootAvailable = withContext(Dispatchers.IO) {
                ServiceLocator.rootAccessClient(requireContext().applicationContext).probe() ==
                    RootAvailability.AVAILABLE
            }
            if (rootAvailable) {
                onProceed()
                return@launch
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.root_permission_title)
                .setMessage(R.string.root_permission_message)
                .setNeutralButton(R.string.btn_dont_show_again) { _, _ ->
                    prefs.edit { putBoolean(PrefManager.PREF_ROOT_DIALOG_DISMISSED, true) }
                    onProceed()
                }
                .setPositiveButton(android.R.string.ok) { _, _ -> onProceed() }
                .show()
        }
    }

    private fun checkRootAndLoad() {
        requestRootThen { probeRootAndLoad(autoFileFallback = true) }
    }

    private fun refreshRootLoad() {
        requestRootThen { probeRootAndLoad(autoFileFallback = false) }
    }

    private fun probeRootAndLoad(autoFileFallback: Boolean) {
        rootProbeJob?.cancel()
        rootProbeJob = viewLifecycleOwner.lifecycleScope.launch {
            val available = withContext(Dispatchers.IO) {
                RootLogcatReader.isAvailable()
            }
            rootDetected = available
            if (available) {
                setRootModeProgrammatic()
                viewModel.loadFromRoot(requireContext(), resolveSelectedBuffer(), crashFilterEnabled())
            } else if (autoFileFallback) {
                setFileModeProgrammatic()
            } else {
                Toast.makeText(requireContext(), R.string.logcat_root_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun beginFileImport() {
        rootProbeJob?.cancel()
        viewModel.cancelActiveLoad()
        pendingFileImport = true
        setFileModeProgrammatic()
        launchFilePicker()
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
        rootProbeJob?.cancel()
        viewModel.cancelActiveLoad()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().use { it.readText() }
                    }
                }
                if (text.isNullOrBlank()) {
                    Toast.makeText(requireContext(), R.string.logcat_file_empty, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (text.length > 5 * 1024 * 1024) {
                    Toast.makeText(requireContext(), R.string.logcat_file_too_large, Toast.LENGTH_LONG).show()
                }
                setFileModeProgrammatic()
                viewModel.loadFromText(text, crashOnly = crashFilterEnabled())
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

        val isRootMode = state.sourceMode == SourceMode.ROOT
        val isFileMode = state.sourceMode == SourceMode.FILE
        if (isFileMode) {
            pendingFileImport = false
        }
        isProgrammaticModeChange = true
        if (isRootMode && !binding.chipModeRoot.isChecked && !pendingFileImport) {
            binding.chipModeRoot.isChecked = true
        } else if (isFileMode && !binding.chipModeFile.isChecked) {
            binding.chipModeFile.isChecked = true
        } else if (pendingFileImport && !binding.chipModeFile.isChecked) {
            binding.chipModeFile.isChecked = true
        }
        isProgrammaticModeChange = false
        binding.bufferChipRow.visibility = if (isRootMode && !pendingFileImport) View.VISIBLE else View.GONE

        if (isRootMode) {
            syncBufferChip(state.activeBuffer)
        }

        if (!isProgrammaticCrashFilterChange && binding.chipCrashOnly.isChecked != state.isFiltered) {
            isProgrammaticCrashFilterChange = true
            binding.chipCrashOnly.isChecked = state.isFiltered
            isProgrammaticCrashFilterChange = false
        }

        syncLevelChips(state.activeLevels)

        val hasData = !state.isLoading && state.entries.isNotEmpty()
        val isEmpty = !state.isLoading && state.entries.isEmpty()
        val hasVisibleEntries = state.displayEntries.isNotEmpty()

        binding.entryCount.visibility = when {
            state.isLoading -> View.GONE
            hasVisibleEntries -> View.VISIBLE
            hasData -> View.VISIBLE
            isEmpty && isRootMode -> View.VISIBLE
            else -> View.GONE
        }

        if (isEmpty && !state.isLoading && !isRootMode) {
            if (rootDetected) {
                EmptyState.bind(
                    binding.emptyState.root,
                    getString(R.string.logcat_empty_root),
                    getString(R.string.logcat_read_action),
                    { refreshRootLoad() },
                    R.drawable.ic_tab_observe,
                )
            } else {
                EmptyState.bind(
                    binding.emptyState.root,
                    getString(R.string.logcat_empty),
                    getString(R.string.logcat_import_action),
                    { launchFilePicker() },
                    R.drawable.ic_tab_observe,
                )
            }
        }

        if (hasData) {
            val visibleCount = state.displayEntries.size
            val levelFilteredCount = state.entries.count { it.level in state.activeLevels }
            val countText = when {
                state.totalRawCount != state.entries.size -> resources.getQuantityString(
                    R.plurals.logcat_entry_count_filtered,
                    visibleCount,
                    visibleCount,
                    state.totalRawCount,
                )
                levelFilteredCount != visibleCount -> resources.getQuantityString(
                    R.plurals.logcat_entry_count_filtered,
                    visibleCount,
                    visibleCount,
                    levelFilteredCount,
                )
                else -> resources.getQuantityString(R.plurals.logcat_entry_count, visibleCount, visibleCount)
            }
            binding.entryCount.text = countText
        } else if (hasData && !hasVisibleEntries) {
            binding.entryCount.text = getString(R.string.logcat_search_no_match)
        } else if (isEmpty && isRootMode) {
            binding.entryCount.text = when {
                state.rootLoadFailed -> getString(R.string.logcat_root_read_failed)
                else -> getString(R.string.logcat_buffer_empty)
            }
        }

        if (state.rootLoadFailed) {
            if (!lastRootLoadFailedShown) {
                lastRootLoadFailedShown = true
                requireContext().showErrorToast(getString(R.string.logcat_root_read_failed)) {
                    viewModel.clearRootLoadFailed()
                    lastRootLoadFailedShown = false
                }
            }
        } else {
            lastRootLoadFailedShown = false
            requireContext().showErrorToast(state.errorMessage) { viewModel.clearError() }
        }
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
            isProgrammaticBufferChange = true
            chip.isChecked = true
            isProgrammaticBufferChange = false
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
            R.id.item_observe_refresh_logcat -> {
                when (viewModel.uiState.value.sourceMode) {
                    SourceMode.FILE -> viewModel.reloadCurrentSource(requireContext(), crashFilterEnabled())
                    else -> refreshRootLoad()
                }
                true
            }
            R.id.item_observe_import_logcat -> {
                beginFileImport()
                true
            }
            else -> false
        }
    }

    companion object {
        const val TAG = "logcat"

        private val LEVEL_CHIP_MAP = mapOf(
            R.id.chipFatal to LogcatLevel.FATAL,
            R.id.chipError to LogcatLevel.ERROR,
            R.id.chipWarning to LogcatLevel.WARNING,
            R.id.chipInfo to LogcatLevel.INFO,
            R.id.chipDebug to LogcatLevel.DEBUG,
        )

        fun newInstance(): LogcatFragment = LogcatFragment()
    }
}
