package nota.android.crash.xp.app.observe

import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.log.CrashLogJsonlStore
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.di.ServiceLocator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Toolbar actions for crash history data (export, filters, retention).
 * Hosted on [ObserveHostFragment] so menu works on History and Stats sub-tabs.
 */
internal class CrashHistoryMenuActions(
    private val fragment: Fragment,
    private val viewModel: CrashHistoryViewModel,
    private val launchSaveZip: (String) -> Unit,
) {

    private var privacyAcknowledgedThisSession = false

    fun prepareMenu(menu: Menu) {
        val filterItem = menu.findItem(R.id.item_observe_filter)
        val hasFilter = viewModel.uiState.value.activeFilter != null
        filterItem?.setTitle(
            if (hasFilter) R.string.observe_menu_filter_clear
            else R.string.observe_menu_filter,
        )

        val pkgFilterItem = menu.findItem(R.id.item_observe_package_filter)
        val activePkg = viewModel.uiState.value.activePackageFilter
        pkgFilterItem?.title = if (activePkg != null) {
            fragment.getString(R.string.observe_menu_package_filter_clear, activePkg)
        } else {
            fragment.getString(R.string.observe_menu_package_filter)
        }
    }

    fun handleItem(item: MenuItem): Boolean {
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
            R.id.item_observe_package_filter -> {
                showPackageFilterDialog()
                true
            }
            R.id.item_observe_export -> {
                exportLogs()
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
            else -> false
        }
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.observe_clear_history_confirm_title)
            .setMessage(R.string.observe_clear_history_confirm_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.clearHistory()
                Toast.makeText(fragment.requireContext(), R.string.observe_clear_history_success, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showFilterDialog() {
        val editText = EditText(fragment.requireContext()).apply {
            hint = fragment.getString(R.string.observe_filter_package_hint)
            setSingleLine()
            setPadding(64, 32, 64, 16)
        }

        MaterialAlertDialogBuilder(fragment.requireContext())
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

    private fun showPackageFilterDialog() {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val packageCounts = viewModel.getPackageCounts()
            if (packageCounts.isEmpty()) {
                Toast.makeText(fragment.requireContext(), R.string.observe_export_no_events, Toast.LENGTH_SHORT).show()
                return@launch
            }

            val items = packageCounts.map { (pkg, count) -> "$pkg ($count)" }.toTypedArray()
            val activePkg = viewModel.uiState.value.activePackageFilter

            MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.observe_menu_package_filter)
                .setItems(items) { _, which ->
                    val selectedPkg = packageCounts[which].first
                    viewModel.setPackageFilter(selectedPkg)
                }
                .setNeutralButton(android.R.string.cancel, null)
                .apply {
                    if (activePkg != null) {
                        setNegativeButton(R.string.observe_package_filter_clear) { _, _ ->
                            viewModel.setPackageFilter(null)
                        }
                    }
                }
                .show()
        }
    }

    private fun exportLogs() {
        if (privacyAcknowledgedThisSession) {
            showExportTypeDialog()
        } else {
            showPrivacyDialog()
        }
    }

    private fun showPrivacyDialog() {
        MaterialAlertDialogBuilder(fragment.requireContext())
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
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.observe_menu_export)
            .setItems(
                arrayOf(
                    fragment.getString(R.string.observe_export_share),
                    fragment.getString(R.string.observe_export_save),
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
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val jsonl = viewModel.exportEvents()
            if (jsonl == null) {
                Toast.makeText(fragment.requireContext(), R.string.observe_export_no_events, Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(fragment.requireContext().cacheDir, "crash_export_$timestamp.jsonl")
                file.writeText(jsonl)

                val uri = FileProvider.getUriForFile(
                    fragment.requireContext(),
                    "${fragment.requireContext().packageName}.fileprovider",
                    file,
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "CrashCenter export ($timestamp)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                fragment.startActivity(Intent.createChooser(intent, fragment.getString(R.string.observe_menu_export)))
            } catch (e: Exception) {
                Log.w("CrashHistoryMenuActions", "Failed to share crash logs", e)
                Toast.makeText(fragment.requireContext(), R.string.observe_export_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveLogsZip() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        launchSaveZip("crash_export_$timestamp.zip")
    }

    fun writeZipToUri(uri: android.net.Uri) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val zipBytes = viewModel.exportZip()
            if (zipBytes == null) {
                Toast.makeText(fragment.requireContext(), R.string.observe_export_no_events, Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                fragment.requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(zipBytes)
                }
                Toast.makeText(fragment.requireContext(), R.string.observe_export_saved, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w("CrashHistoryMenuActions", "Failed to write zip to URI", e)
                Toast.makeText(fragment.requireContext(), R.string.observe_export_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRetentionDialog() {
        val prefs = ServiceLocator.prefs(fragment.requireContext())
        val currentMaxEntries = prefs.getInt(
            PrefManager.PREF_CRASH_LOG_MAX_ENTRIES,
            CrashLogJsonlStore.DEFAULT_MAX_ENTRIES,
        )
        val currentMaxBytesMb = prefs.getLong(
            PrefManager.PREF_CRASH_LOG_MAX_BYTES,
            CrashLogJsonlStore.DEFAULT_MAX_BYTES,
        ) / (1024L * 1024L)

        val density = fragment.resources.displayMetrics.density
        val dp16 = (16 * density).toInt()
        val dp8 = (8 * density).toInt()

        val entriesInput = EditText(fragment.requireContext()).apply {
            hint = fragment.getString(R.string.retention_max_entries_label)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentMaxEntries.toString())
            setPadding(dp16, dp8, dp16, dp8)
        }

        val bytesInput = EditText(fragment.requireContext()).apply {
            hint = fragment.getString(R.string.retention_max_bytes_label)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(currentMaxBytesMb.toString())
            setPadding(dp16, dp8, dp16, dp8)
        }

        val container = LinearLayout(fragment.requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16, dp16, dp16, dp8)
            addView(entriesInput)
            addView(bytesInput)
        }

        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.retention_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val maxEntries = entriesInput.text.toString().toIntOrNull()?.coerceIn(1, 100_000)
                    ?: CrashLogJsonlStore.DEFAULT_MAX_ENTRIES
                val maxBytesMb = bytesInput.text.toString().toLongOrNull()?.coerceIn(1, 1024)
                    ?: (CrashLogJsonlStore.DEFAULT_MAX_BYTES / (1024L * 1024L))
                val maxBytes = maxBytesMb * 1024L * 1024L

                prefs.edit {
                    putInt(PrefManager.PREF_CRASH_LOG_MAX_ENTRIES, maxEntries)
                    putLong(PrefManager.PREF_CRASH_LOG_MAX_BYTES, maxBytes)
                }

                CrashLogJsonlStore.maxEntries = maxEntries
                CrashLogJsonlStore.maxBytes = maxBytes

                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        ServiceLocator.crashLogRepository(fragment.requireContext()).applyRetention()
                    }
                    Toast.makeText(fragment.requireContext(), R.string.retention_saved, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
