package nota.android.crash.xp.app.common.ui

import android.view.View
import nota.android.crash.xp.app.databinding.ViewPermissionBannerBinding

object PermissionBanner {

    fun bind(root: View, visible: Boolean, title: CharSequence? = null, compact: Boolean = false) {
        root.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) return
        val binding = ViewPermissionBannerBinding.bind(root)
        binding.permissionBannerTitle.apply {
            if (title != null) text = title
            maxLines = if (compact) 1 else 2
        }
        binding.permissionGrantButton.visibility =
            if (compact) View.GONE else View.VISIBLE
    }

    fun setOnActionClickListener(root: View, listener: View.OnClickListener?) {
        val binding = ViewPermissionBannerBinding.bind(root)
        val openSettingsFlow = listener
        root.setOnClickListener(openSettingsFlow)
        binding.permissionGrantButton.setOnClickListener(openSettingsFlow)
    }
}
