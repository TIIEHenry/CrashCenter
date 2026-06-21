package nota.android.crash.xp.app.config

import android.view.View
import nota.android.crash.xp.app.ModuleActivation
import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.PermissionBanner
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

data class PermissionBannerDecision(
    val isPlural: Boolean,
    val pluralResId: Int,
    val count: Int,
    val stringResId: Int,
    val compact: Boolean,
)

fun decideTitle(
    status: PackageVisibilityHelper.Status,
    isModuleActive: Boolean = ModuleActivation.isModuleActive(),
): PermissionBannerDecision {
    val compact = !isModuleActive
    return if (status.visiblePackageCount > 0) {
        PermissionBannerDecision(
            isPlural = true,
            pluralResId = if (compact) R.plurals.permission_list_partial_compact else R.plurals.permission_list_partial,
            count = status.visiblePackageCount,
            stringResId = 0,
            compact = compact,
        )
    } else {
        PermissionBannerDecision(
            isPlural = false,
            pluralResId = 0,
            count = 0,
            stringResId = if (compact) R.string.permission_banner_title_compact else R.string.permission_banner_title,
            compact = compact,
        )
    }
}

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
        val decision = decideTitle(status)
        val title = if (decision.isPlural) {
            binding.root.context.resources.getQuantityString(
                decision.pluralResId,
                decision.count,
                decision.count,
            )
        } else {
            binding.root.context.getString(decision.stringResId)
        }
        PermissionBanner.bind(binding.permissionBanner.root, status.needsUserAction, title, decision.compact)
    }
}
