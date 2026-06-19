package nota.android.crash.xp.app.common.ui

import android.view.View
import nota.android.crash.xp.app.SystemBars

object ToolbarHeaderInsets {

    fun apply(toolbarHeader: View) {
        SystemBars.applyToolbarHeaderInsets(toolbarHeader)
    }
}
