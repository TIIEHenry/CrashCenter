package nota.android.crash.xp.app.config

import android.content.Context
import android.widget.Toast
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.ModuleActivation
import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.R

internal class ConfigDialogHelper(
    private val context: Context,
    private val onPermissionSettingsOpened: () -> Unit,
) {

    fun showHelpDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.using_warning_title))
            .setMessage(context.getString(R.string.using_warning))
            .show()
    }

    fun showXposedInactiveDialogIfNeeded(xposedDialogShown: Boolean): Boolean {
        if (xposedDialogShown) return false
        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PrefManager.PREF_XPOSED_DIALOG_DISMISSED, false)) return false
        if (ModuleActivation.isModuleActive()) return false

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.xposed_not_active)
            .setMessage(R.string.xposed_hint)
            .setNeutralButton(R.string.btn_dont_show_again) { _, _ ->
                prefs.edit { putBoolean(PrefManager.PREF_XPOSED_DIALOG_DISMISSED, true) }
            }
            .show()
        return true
    }

    fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.permission_rationale_title)
            .setMessage(R.string.permission_rationale_message)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                onPermissionSettingsOpened()
                if (!PackageVisibilityHelper.openAppSettings(context)) {
                    Toast.makeText(context, R.string.permission_settings_failed, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
