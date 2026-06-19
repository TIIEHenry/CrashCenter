package nota.android.crash.xp.app.common.ui

import android.view.View
import android.widget.TextView
import nota.android.crash.xp.app.R

object PermissionBanner {

    @JvmStatic
    @JvmOverloads
    fun bind(root: View, visible: Boolean, title: CharSequence? = null, compact: Boolean = false) {
        root.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) return
        root.findViewById<TextView>(R.id.permissionBannerTitle)?.apply {
            if (title != null) text = title
            maxLines = if (compact) 1 else 2
        }
        root.findViewById<View>(R.id.permissionGrantButton)?.visibility =
            if (compact) View.GONE else View.VISIBLE
    }

    @JvmStatic
    fun setOnActionClickListener(root: View, listener: View.OnClickListener?) {
        val openSettingsFlow = listener
        root.setOnClickListener(openSettingsFlow)
        root.findViewById<View>(R.id.permissionGrantButton)?.setOnClickListener(openSettingsFlow)
    }
}
