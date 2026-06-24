package nota.android.crash.xp.app.config

import android.view.Menu
import android.view.MenuItem
import nota.android.crash.xp.app.R

internal class ConfigOptionsMenuHelper(
    private val viewModel: ConfigViewModel,
    private val showAddManagedAppSheet: () -> Unit,
    private val showHelpDialog: () -> Unit,
    private val showTestCrashDialog: () -> Unit,
) {

    fun prepareOptionsMenu(menu: Menu, listCount: Int) {
        // No-op: all filtering/sorting is in the UI chips, menu keeps help + test crash
    }

    fun handleOptionsItem(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_help -> {
                showHelpDialog()
                true
            }
            R.id.item_test_crash -> {
                showTestCrashDialog()
                true
            }
            else -> {
                val sortMode = SORT_MODE_MAP[item.itemId] ?: return false
                item.isChecked = true
                viewModel.setSortMode(sortMode)
                true
            }
        }
    }

    companion object {
        val SORT_MODE_MAP = mapOf(
            R.id.item_sort_by_name to SortMode.NAME_ASC,
            R.id.item_sort_by_name_reverse to SortMode.NAME_DESC,
            R.id.item_sort_by_install_time to SortMode.INSTALL_TIME_ASC,
            R.id.item_sort_by_install_time_reverse to SortMode.INSTALL_TIME_DESC,
            R.id.item_sort_by_update_time to SortMode.UPDATE_TIME_ASC,
            R.id.item_sort_by_update_time_reverse to SortMode.UPDATE_TIME_DESC,
        )
    }
}
