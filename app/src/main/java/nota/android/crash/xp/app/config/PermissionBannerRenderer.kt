package nota.android.crash.xp.app.config

import android.view.View
import nota.android.crash.xp.app.ModuleActivation
import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.PermissionBanner
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

class PermissionBannerRenderer(
    private val binding: FragmentConfigBinding,
    private val onActionClick: () -> Unit,
) {
    init {
        PermissionBanner.setOnActionClickListener(binding.permissionBanner.root, View.OnClickListener {
            onActionClick()
        })
    }

    fun render(status: PackageVisibilityHelper.Status) {
        val compact = !ModuleActivation.isModuleActive()
        val title = if (status.visiblePackageCount > 0) {
            binding.root.context.getString(
                if (compact) R.string.permission_list_partial_hint_compact else R.string.permission_list_partial_hint,
                status.visiblePackageCount,
            )
        } else {
            binding.root.context.getString(
                if (compact) R.string.permission_banner_title_compact else R.string.permission_banner_title,
            )
        }
        PermissionBanner.bind(binding.permissionBanner.root, status.needsUserAction, title, compact)
    }
}
