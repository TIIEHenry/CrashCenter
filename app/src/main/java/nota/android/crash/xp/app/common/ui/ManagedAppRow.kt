package nota.android.crash.xp.app.common.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.config.InterventionStatus
import nota.android.crash.xp.app.config.ManagedApp

object ManagedAppRow {

    fun bind(
        root: View,
        app: ManagedApp,
        context: Context,
        onSwitchChanged: ((ManagedApp, Boolean) -> Unit)?,
    ) {
        root.contentDescription = context.getString(
            R.string.legacy_app_row_a11y,
            app.label,
            app.packageName,
        )
        root.findViewById<ImageView>(R.id.ivIcon)?.setImageDrawable(app.icon)
        root.findViewById<TextView>(R.id.tvName)?.text = app.label
        root.findViewById<TextView>(R.id.tvSubtitle)?.text = app.packageName

        val badge = root.findViewById<TextView>(R.id.tvStatusBadge)
        when (app.interventionStatus) {
            InterventionStatus.ENABLED -> {
                badge.visibility = View.VISIBLE
                badge.setBackgroundResource(R.drawable.bg_status_active)
                badge.setTextColor(context.themeColor(R.attr.statusBannerActiveTextColor))
                badge.text = context.getString(R.string.managed_status_enabled)
            }
            InterventionStatus.PENDING -> {
                badge.visibility = View.VISIBLE
                badge.setBackgroundResource(R.drawable.bg_status_inactive)
                badge.setTextColor(context.themeColor(R.attr.statusBannerInactiveTextColor))
                badge.text = context.getString(R.string.managed_status_pending)
            }
        }

        val switchView = root.findViewById<SwitchMaterial>(R.id.sw) ?: return
        switchView.contentDescription = context.getString(
            if (app.switchChecked) {
                R.string.switch_disable_intervention
            } else {
                R.string.switch_enable_intervention
            },
        )
        switchView.setOnCheckedChangeListener(null)
        switchView.isChecked = app.switchChecked
        switchView.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChanged?.invoke(app, isChecked)
        }
    }
}
