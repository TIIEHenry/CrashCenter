package nota.android.crash.xp.app.observe

import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nota.android.crash.xp.app.R

/**
 * Toolbar actions for [PerAppCrashActivity] (launch app, clear records).
 */
internal class PerAppCrashMenuActions(
    private val activity: AppCompatActivity,
    private val packageName: String?,
    private val getAppLabel: () -> String,
    private val onClearConfirmed: () -> Unit,
) {

    fun prepareMenu(menu: Menu) {
        val launchItem = menu.findItem(R.id.item_launch_app)
        if (packageName == null) {
            launchItem?.isVisible = false
        } else {
            launchItem?.isVisible = true
            launchItem?.isEnabled = isLaunchable()
        }
    }

    fun handleItem(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_launch_app -> {
                launchApp()
                true
            }
            R.id.item_clear_per_app -> {
                showClearDialog()
                true
            }
            else -> false
        }
    }

    private fun isLaunchable(): Boolean {
        val pkg = packageName ?: return false
        return activity.packageManager.getLaunchIntentForPackage(pkg) != null
    }

    private fun launchApp() {
        val pkg = packageName ?: return
        val intent = activity.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            activity.invalidateMenu()
            Toast.makeText(activity, R.string.per_app_launch_failed, Toast.LENGTH_SHORT).show()
            return
        }
        activity.startActivity(intent)
    }

    private fun showClearDialog() {
        val label = getAppLabel()
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.per_app_clear_confirm_title)
            .setMessage(activity.getString(R.string.per_app_clear_confirm_message, label))
            .setPositiveButton(android.R.string.ok) { _, _ -> onClearConfirmed() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
