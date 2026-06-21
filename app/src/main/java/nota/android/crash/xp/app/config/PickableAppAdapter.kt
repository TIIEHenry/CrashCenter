package nota.android.crash.xp.app.config

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.common.ui.adapter.BaseListAdapter
import nota.android.crash.xp.app.common.ui.adapter.SimpleDiffCallback
import nota.android.crash.xp.app.databinding.ItemPickableAppBinding

class PickableAppAdapter : BaseListAdapter<PickableApp, PickableAppAdapter.VH>(
    SimpleDiffCallback { it.packageName }
) {

    private val selectedPackages = linkedSetOf<String>()

    fun toggleSelection(app: PickableApp) {
        if (!selectedPackages.add(app.packageName)) {
            selectedPackages.remove(app.packageName)
        }
        // Refresh the item to update checkbox state
        val pos = currentList.indexOfFirst { it.packageName == app.packageName }
        if (pos != -1) {
            notifyItemChanged(pos)
        }
    }

    fun selectedPackages(): Set<String> = selectedPackages.toSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPickableAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemPickableAppBinding) :
        BaseViewHolder<PickableApp>(binding.root) {

        init {
            binding.root.setOnClickListener {
                notifyItemClick(binding.root, bindingAdapterPosition)
            }
        }

        override fun bind(data: PickableApp) {
            data.bindAppInfo(binding.root, binding.ivIcon, binding.tvName, binding.tvPackageName)
            binding.checkbox.isChecked = selectedPackages.contains(data.packageName)
        }
    }
}
