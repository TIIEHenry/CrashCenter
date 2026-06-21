package nota.android.crash.xp.app.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.adapter.BaseListAdapter
import nota.android.crash.xp.app.common.ui.adapter.SimpleDiffCallback
import nota.android.crash.xp.app.common.ui.themeColor
import nota.android.crash.xp.app.databinding.ViewManagedAppRowBinding

class ManagedAppAdapter : BaseListAdapter<ManagedApp, ManagedAppAdapter.VH>(
    SimpleDiffCallback { it.packageName }
) {

    var onSwitchChanged: ((ManagedApp, Boolean) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ViewManagedAppRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ViewManagedAppRowBinding) :
        BaseViewHolder<ManagedApp>(binding.root) {

        init {
            binding.root.setOnClickListener {
                notifyItemClick(binding.root, bindingAdapterPosition)
            }
            binding.root.setOnLongClickListener {
                notifyItemLongClick(binding.root, bindingAdapterPosition)
            }
        }

        override fun bind(app: ManagedApp) {
            app.bindAppInfo(binding.root, binding.ivIcon, binding.tvName, binding.tvSubtitle)
            val context = binding.root.context

            when (app.interventionStatus) {
                InterventionStatus.ENABLED -> {
                    binding.tvStatusBadge.visibility = View.VISIBLE
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active)
                    binding.tvStatusBadge.setTextColor(context.themeColor(R.attr.statusBannerActiveTextColor))
                    binding.tvStatusBadge.text = context.getString(R.string.managed_status_enabled)
                }
                InterventionStatus.PENDING -> {
                    binding.tvStatusBadge.visibility = View.VISIBLE
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_inactive)
                    binding.tvStatusBadge.setTextColor(context.themeColor(R.attr.statusBannerInactiveTextColor))
                    binding.tvStatusBadge.text = context.getString(R.string.managed_status_pending)
                }
            }

            binding.sw.contentDescription = context.getString(
                if (app.switchChecked) {
                    R.string.switch_disable_intervention
                } else {
                    R.string.switch_enable_intervention
                },
            )
            binding.sw.setOnCheckedChangeListener(null)
            binding.sw.isChecked = app.switchChecked
            binding.sw.setOnCheckedChangeListener { _, isChecked ->
                onSwitchChanged?.invoke(app, isChecked)
            }
        }
    }
}
