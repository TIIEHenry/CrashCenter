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

    fun prepareOptionsMenu(menu: Menu, isLegacyMode: Boolean?, managedListCount: Int) {
        menu.findItem(R.id.item_select_all)?.isVisible = isLegacyMode == true
        menu.findItem(R.id.item_cancel_all)?.isVisible = isLegacyMode == true
        menu.findItem(R.id.item_add_managed_app)?.isVisible = isLegacyMode == false && managedListCount > 0
    }

    fun handleOptionsItem(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_add_managed_app -> {
                showAddManagedAppSheet()
                true
            }
            R.id.item_cancel_all -> {
                viewModel.selectAll(enabled = false)
                true
            }
            R.id.item_select_all -> {
                viewModel.selectAll(enabled = true)
                true
            }
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
        private val SORT_MODE_MAP = mapOf(
            R.id.item_sort_by_name to SortMode.NAME_ASC,
            R.id.item_sort_by_name_reverse to SortMode.NAME_DESC,
            R.id.item_sort_by_install_time to SortMode.INSTALL_TIME_ASC,
            R.id.item_sort_by_install_time_reverse to SortMode.INSTALL_TIME_DESC,
            R.id.item_sort_by_update_time to SortMode.UPDATE_TIME_ASC,
            R.id.item_sort_by_update_time_reverse to SortMode.UPDATE_TIME_DESC,
        )
    }
}
