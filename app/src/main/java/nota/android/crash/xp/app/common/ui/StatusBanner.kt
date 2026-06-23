package nota.android.crash.xp.app.common.ui

import android.view.View
import nota.android.crash.root.RootAvailability
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.ViewStatusBannerBinding

object StatusBanner {

    fun bind(
        root: View,
        active: Boolean,
        rootAvailability: RootAvailability? = null,
        activeBackendCount: Int = 0,
        totalBackendCount: Int = 0,
    ) {
        val binding = ViewStatusBannerBinding.bind(root)
        val context = root.context
        val colorRes = if (active) {
            root.setBackgroundResource(R.drawable.bg_status_active)
            binding.statusIcon.setImageResource(R.drawable.ic_shield_check)
            binding.statusTitle.setText(R.string.xposed_active_inline)
            R.attr.statusBannerActiveTextColor
        } else {
            root.setBackgroundResource(R.drawable.bg_status_inactive)
            binding.statusIcon.setImageResource(R.drawable.ic_shield_off)
            binding.statusTitle.setText(R.string.xposed_inactive_inline)
            R.attr.statusBannerInactiveTextColor
        }
        val color = context.themeColor(colorRes)
        binding.statusTitle.setTextColor(color)
        binding.statusSubtitle.setTextColor(color)

        // Build root/backend subtitle line
        val subtitle = buildSubtitle(rootAvailability, activeBackendCount, totalBackendCount)
        if (subtitle.isNullOrEmpty()) {
            binding.statusSubtitle.visibility = View.GONE
        } else {
            binding.statusSubtitle.visibility = View.VISIBLE
            binding.statusSubtitle.text = subtitle
        }
    }

    fun setOnClickListener(root: View, listener: View.OnClickListener?) {
        root.setOnClickListener(listener)
    }

    private fun buildSubtitle(
        rootAvailability: RootAvailability?,
        activeBackendCount: Int,
        totalBackendCount: Int,
    ): String? {
        val parts = mutableListOf<String>()
        if (rootAvailability != null) {
            parts.add(
                when (rootAvailability) {
                    RootAvailability.AVAILABLE -> "Root: Available"
                    RootAvailability.DENIED -> "Root: Denied"
                    RootAvailability.UNAVAILABLE -> "Root: Unavailable"
                }
            )
        }
        if (totalBackendCount > 0) {
            parts.add("Backends: $activeBackendCount/$totalBackendCount")
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}
