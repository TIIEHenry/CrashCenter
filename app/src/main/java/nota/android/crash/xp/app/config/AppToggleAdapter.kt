package nota.android.crash.xp.app.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.adapter.BaseListAdapter
import nota.android.crash.xp.app.common.ui.adapter.SimpleDiffCallback
import nota.android.crash.xp.app.databinding.ActivityMainAppitemBinding

class AppToggleAdapter(
    private val onToggle: (AppItem) -> Unit,
) : BaseListAdapter<AppItem, AppToggleAdapter.VH>(
    SimpleDiffCallback { it.packageName },
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ActivityMainAppitemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
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
            binding.sw.setOnCheckedChangeListener { _, isChecked ->
                val item = getItem(bindingAdapterPosition)
                if (item != null && item.interceptEnabled != isChecked) {
                    onToggle(item)
                }
            }
        }

        override fun bind(data: AppItem) {
            data.bindAppInfo(binding.root, binding.ivIcon, binding.tvName, binding.tvPackageName)
            val isSelf = PackageInfoLoader.isItself(data.packageName)
            binding.sw.isEnabled = !isSelf
            binding.sw.isChecked = data.interceptEnabled
            binding.tvSystemBadge.visibility =
                if (data.isSystem) View.VISIBLE else View.GONE
        }
    }
}
