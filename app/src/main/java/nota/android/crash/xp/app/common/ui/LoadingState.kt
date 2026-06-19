package nota.android.crash.xp.app.common.ui

import android.view.View
import android.widget.TextView
import nota.android.crash.xp.app.R

object LoadingState {

    @JvmStatic
    fun bind(root: View, message: CharSequence? = null) {
        if (message != null) {
            root.findViewById<TextView>(R.id.loadingMessage)?.text = message
        }
    }
}
