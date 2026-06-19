package tiiehenry.celestialruler.ui.interaction

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.view.InputDevice

/**
 * Resolves [InputModality] from device / host configuration.
 *
 * Manager UI defaults to [InputModality.TouchPrimary] unless PC / desktop / pointer
 * signals are present. Hook overlay should call [resolve] against the **host** Context.
 */
object InputModalityResolver {

    @JvmStatic
    fun resolve(context: Context): InputModality {
        val config = context.resources.configuration
        return if (isPointerPrimaryEnvironment(config, context)) {
            InputModality.PointerPrimary
        } else {
            InputModality.TouchPrimary
        }
    }

    @JvmStatic
    fun isPointerPrimaryEnvironment(config: Configuration, context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_PC)) {
                return true
            }
        }
        if (config.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_DESK) {
            return true
        }
        if (config.keyboard != Configuration.KEYBOARD_UNDEFINED &&
            config.keyboard != Configuration.KEYBOARD_NOKEYS &&
            config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
        ) {
            return true
        }
        if (hasExternalMouse(context)) {
            return true
        }
        return false
    }

    private fun hasExternalMouse(context: Context): Boolean {
        val ids = InputDevice.getDeviceIds()
        for (id in ids) {
            val device = InputDevice.getDevice(id) ?: continue
            if (!device.isExternal) continue
            if (device.supportsSource(InputDevice.SOURCE_MOUSE)) {
                return true
            }
        }
        return false
    }
}
