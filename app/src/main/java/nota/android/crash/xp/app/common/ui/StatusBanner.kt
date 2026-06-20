package nota.android.crash.xp.app.common.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.ViewStatusBannerBinding

object StatusBanner {

    fun bind(root: View, active: Boolean) {
        val binding = ViewStatusBannerBinding.bind(root)
        val context = root.context
        if (active) {
            root.setBackgroundResource(R.drawable.bg_status_active)
            binding.statusIcon.setImageResource(R.drawable.ic_shield_check)
            binding.statusTitle.apply {
                setText(R.string.xposed_active_inline)
                setTextColor(context.themeColor(R.attr.statusBannerActiveTextColor))
            }
        } else {
            root.setBackgroundResource(R.drawable.bg_status_inactive)
            binding.statusIcon.setImageResource(R.drawable.ic_shield_off)
            binding.statusTitle.apply {
                setText(R.string.xposed_inactive_inline)
                setTextColor(context.themeColor(R.attr.statusBannerInactiveTextColor))
            }
        }
    }

    fun setOnClickListener(root: View, listener: View.OnClickListener?) {
        root.setOnClickListener(listener)
    }
}
