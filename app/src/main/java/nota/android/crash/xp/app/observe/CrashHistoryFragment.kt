package nota.android.crash.xp.app.observe

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nota.android.crash.log.CanonicalJsonlWriter
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.di.crashHistoryViewModelFactory
import nota.android.crash.xp.app.databinding.FragmentCrashHistoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHistoryFragment : Fragment() {

    private var _binding: FragmentCrashHistoryBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private val viewModel: CrashHistoryViewModel by viewModels {
        ServiceLocator.crashHistoryViewModelFactory(requireContext())
    }

    private lateinit var adapter: CrashHistoryPagingAdapter
    private var lastHistoryCleared = 0
    private var privacyAcknowledgedThisSession = false

    private lateinit var saveZipLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveZipLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip"),
        ) { uri ->
            if (uri != null) {
                writeZipToUri(uri)
            }
        }
    }

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
        if (state.historyCleared != lastHistoryCleared) {
            lastHistoryCleared = state.historyCleared
            adapter.refresh()
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
                exportLogs()
                true
            }
            R.id.item_observe_stats -> {
                showStatistics()
                true
            }
            R.id.item_observe_retention -> {
                showRetentionDialog()
                true
            }
            R.id.item_clear_history -> {
                showClearHistoryDialog()
                true
            }
            R.id.item_observe_import_logcat -> {
                navigateToLogcatTab()
                true
            }
            else -> false
        }
    }

    private fun navigateToLogcatTab() {
        val host = parentFragment as? ObserveHostFragment ?: return
        val tabLayout = host.view?.findViewById<com.google.android.material.tabs.TabLayout>(
            R.id.tabLayout,
        ) ?: return
        // TAB_LOGCAT = 2
        tabLayout.selectTab(tabLayout.getTabAt(2))
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.observe_clear_history_confirm_title)
            .setMessage(R.string.observe_clear_history_confirm_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.clearHistory()
                Toast.makeText(requireContext(), R.string.observe_clear_history_success, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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

    private fun exportLogs() {
        if (privacyAcknowledgedThisSession) {
            showExportTypeDialog()
        } else {
            showPrivacyDialog()
        }
    }

    private fun showPrivacyDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.observe_export_privacy_title)
            .setMessage(R.string.observe_export_privacy_message)
            .setPositiveButton(R.string.observe_export_continue) { _, _ ->
                privacyAcknowledgedThisSession = true
                showExportTypeDialog()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showExportTypeDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.observe_menu_export)
            .setItems(
                arrayOf(
                    getString(R.string.observe_export_share),
                    getString(R.string.observe_export_save),
                ),
            ) { _, which ->
                when (which) {
                    0 -> shareLogs()
                    1 -> saveLogsZip()
                }
            }
            .show()
    }

    private fun shareLogs() {
        viewLifecycleOwner.lifecycleScope.launch {
            val jsonl = viewModel.exportEvents()
            if (jsonl == null) {
                Toast.makeText(requireContext(), R.string.observe_export_no_events, Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(requireContext().cacheDir, "crash_export_$timestamp.jsonl")
                file.writeText(jsonl)

                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file,
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "CrashCenter export ($timestamp)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.observe_menu_export)))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.observe_export_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveLogsZip() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        saveZipLauncher.launch("crash_export_$timestamp.zip")
    }

    private fun writeZipToUri(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val zipBytes = viewModel.exportZip()
            if (zipBytes == null) {
                Toast.makeText(requireContext(), R.string.observe_export_no_events, Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(zipBytes)
                }
                Toast.makeText(requireContext(), R.string.observe_export_saved, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.observe_export_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            val stats = viewModel.getStatistics()
            if (stats.totalCount == 0) {
                Toast.makeText(requireContext(), R.string.observe_export_no_events, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val recentFormatted = dateFormat.format(Date(stats.mostRecentTimestampMs))

            val sb = StringBuilder()
            sb.appendLine(getString(R.string.observe_stats_total, stats.totalCount))
            sb.appendLine(getString(R.string.observe_stats_unique_packages, stats.uniquePackageCount))
            sb.appendLine(getString(R.string.observe_stats_most_recent, recentFormatted))
            sb.appendLine()
            sb.appendLine(getString(R.string.observe_stats_top_packages))
            stats.topPackages.forEach { (pkg, count) ->
                sb.appendLine(getString(R.string.observe_stats_package_entry, pkg, count))
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.observe_stats_title)
                .setMessage(sb.toString().trim())
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun showRetentionDialog() {
        val prefs = ServiceLocator.prefs(requireContext())
        val currentMaxEntries = prefs.getInt(
            PrefManager.PREF_CRASH_LOG_MAX_ENTRIES,
            CanonicalJsonlWriter.DEFAULT_MAX_ENTRIES,
        )
        val currentMaxBytesMb = prefs.getLong(
            PrefManager.PREF_CRASH_LOG_MAX_BYTES,
            CanonicalJsonlWriter.DEFAULT_MAX_BYTES,
        ) / (1024L * 1024L)

        val dp16 = (16 * resources.displayMetrics.density).toInt()
        val dp8 = (8 * resources.displayMetrics.density).toInt()

        val entriesInput = EditText(requireContext()).apply {
            hint = getString(R.string.retention_max_entries_label)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentMaxEntries.toString())
            setPadding(dp16, dp8, dp16, dp8)
        }

        val bytesInput = EditText(requireContext()).apply {
            hint = getString(R.string.retention_max_bytes_label)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(currentMaxBytesMb.toString())
            setPadding(dp16, dp8, dp16, dp8)
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16, dp16, dp16, dp8)
            addView(entriesInput)
            addView(bytesInput)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.retention_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val maxEntries = entriesInput.text.toString().toIntOrNull()?.coerceIn(1, 100_000)
                    ?: CanonicalJsonlWriter.DEFAULT_MAX_ENTRIES
                val maxBytesMb = bytesInput.text.toString().toLongOrNull()?.coerceIn(1, 1024)
                    ?: (CanonicalJsonlWriter.DEFAULT_MAX_BYTES / (1024L * 1024L))
                val maxBytes = maxBytesMb * 1024L * 1024L

                prefs.edit {
                    putInt(PrefManager.PREF_CRASH_LOG_MAX_ENTRIES, maxEntries)
                    putLong(PrefManager.PREF_CRASH_LOG_MAX_BYTES, maxBytes)
                }

                // Sync volatile fields for hook-side append()
                CanonicalJsonlWriter.maxEntries = maxEntries
                CanonicalJsonlWriter.maxBytes = maxBytes

                Toast.makeText(requireContext(), R.string.retention_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val TAG = "crash_history"

        fun newInstance(): CrashHistoryFragment = CrashHistoryFragment()
    }
}
