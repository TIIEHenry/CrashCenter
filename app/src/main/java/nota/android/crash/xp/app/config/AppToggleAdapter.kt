package nota.android.crash.xp.app.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nota.android.crash.xp.app.common.ui.adapter.BaseListAdapter
import nota.android.crash.xp.app.common.ui.adapter.SimpleDiffCallback
import nota.android.crash.xp.app.databinding.ActivityMainAppitemBinding

class AppToggleAdapter : BaseListAdapter<AppItem, AppToggleAdapter.VH>(
    SimpleDiffCallback { it.packageName }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ActivityMainAppitemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ActivityMainAppitemBinding) :
        BaseViewHolder<AppItem>(binding.root) {

        init {
            binding.root.setOnClickListener {
                notifyItemClick(binding.root, bindingAdapterPosition)
            }
        }

        override fun bind(data: AppItem) {
            data.bindAppInfo(binding.root, binding.ivIcon, binding.tvName, binding.tvPackageName)
            val context = binding.root.context
            binding.sw.contentDescription = context.getString(
                if (data.hookEnabled) {
                    nota.android.crash.xp.app.R.string.switch_disable_intervention
                } else {
                    nota.android.crash.xp.app.R.string.switch_enable_intervention
                },
            )
            binding.sw.isChecked = data.hookEnabled
            binding.tvSystemBadge.visibility =
                if (data.isSystem) View.VISIBLE else View.GONE
        }
    }
}
