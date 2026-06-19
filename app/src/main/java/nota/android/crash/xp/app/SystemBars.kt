package nota.android.crash.xp.app

import android.content.res.Configuration
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

object SystemBars {

    @JvmStatic
    fun setup(activity: AppCompatActivity) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val isLightTheme = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
            Configuration.UI_MODE_NIGHT_YES
        controller.isAppearanceLightStatusBars = isLightTheme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.isAppearanceLightNavigationBars = isLightTheme
        }
    }

    @JvmStatic
    fun applyToolbarHeaderInsets(toolbarHeader: View) {
        ViewCompat.setOnApplyWindowInsetsListener(toolbarHeader) { view, windowInsets ->
            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = statusBarInsets.top)
            windowInsets
        }
    }
}
