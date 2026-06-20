package nota.android.crash.xp.app.config

import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.LoadingState

internal class ConfigOptionsMenuHelper(
    private val showTestToastAndCrash: () -> Unit,
    private val viewModel: ConfigViewModel,
    private val showAddManagedAppSheet: () -> Unit,
    private val showHelpDialog: () -> Unit,
) {

    fun prepareOptionsMenu(menu: Menu, isLegacyMode: Boolean?) {
        menu.findItem(R.id.item_select_all)?.isVisible = isLegacyMode == true
        menu.findItem(R.id.item_cancel_all)?.isVisible = isLegacyMode == true
        menu.findItem(R.id.item_add_managed_app)?.isVisible = isLegacyMode == false
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
            R.id.item_test -> {
                showTestToastAndCrash()
                true
            }
            R.id.item_sort_by_name -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.NAME_ASC)
                true
            }
            R.id.item_sort_by_name_reverse -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.NAME_DESC)
                true
            }
            R.id.item_sort_by_install_time -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.INSTALL_TIME_ASC)
                true
            }
            R.id.item_sort_by_install_time_reverse -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.INSTALL_TIME_DESC)
                true
            }
            R.id.item_sort_by_update_time -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.UPDATE_TIME_ASC)
                true
            }
            R.id.item_sort_by_update_time_reverse -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.UPDATE_TIME_DESC)
                true
            }
            else -> false
        }
    }
}
