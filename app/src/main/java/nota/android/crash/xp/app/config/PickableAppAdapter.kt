package nota.android.crash.xp.app.config

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.databinding.ItemPickableAppBinding

class PickableAppAdapter : ListAdapter<PickableApp, PickableAppAdapter.VH>(DiffCallback()) {

    private val selectedPackages = linkedSetOf<String>()
    private var onItemClickListener: ((PickableApp) -> Unit)? = null

    fun onItemClick(f: (PickableApp) -> Unit) {
        onItemClickListener = f
    }

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

    fun clearSelection() {
        selectedPackages.clear()
        notifyItemRangeChanged(0, itemCount)
    }

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
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(getItem(pos))
                }
            }
        }

        fun bind(data: PickableApp) {
            binding.ivIcon.setImageDrawable(data.icon)
            binding.tvName.text = data.label
            binding.tvPackageName.text = data.packageName
            binding.checkbox.isChecked = selectedPackages.contains(data.packageName)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PickableApp>() {
        override fun areItemsTheSame(oldItem: PickableApp, newItem: PickableApp): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: PickableApp, newItem: PickableApp): Boolean {
            return oldItem == newItem
        }
    }
}
