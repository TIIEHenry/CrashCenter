package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import nota.android.crash.xp.app.R

interface AppListItem {
    val packageName: String
    val label: String
    val appInfo: ApplicationInfo
    val isSystem: Boolean
    val updateTime: Long
    val installTime: Long
}

fun AppListItem.bindAppInfo(
    rootView: View,
    ivIcon: ImageView,
    tvName: TextView,
    tvPackageName: TextView,
) {
    val pm = rootView.context.packageManager
    rootView.contentDescription = rootView.context.getString(
        R.string.legacy_app_row_a11y, label, packageName,
        if (isSystem) rootView.context.getString(R.string.system_app_badge) else "",
    )
    ivIcon.setImageDrawable(try {
        appInfo.loadIcon(pm)
    } catch (_: Exception) {
        pm.getDrawable("android", android.R.drawable.sym_def_app_icon, null)
    })
    tvName.text = label
    tvPackageName.text = packageName
}
