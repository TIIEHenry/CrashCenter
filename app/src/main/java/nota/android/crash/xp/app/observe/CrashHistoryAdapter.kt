package nota.android.crash.xp.app.observe

import android.view.LayoutInflater
import android.view.ViewGroup
import nota.android.crash.xp.app.common.ui.adapter.BaseListAdapter
import nota.android.crash.xp.app.common.ui.adapter.SimpleDiffCallback
import nota.android.crash.xp.app.data.CrashEvent
import nota.android.crash.xp.app.databinding.ViewCrashEventRowBinding

class CrashHistoryAdapter : BaseListAdapter<CrashEvent, CrashEventViewHolder>(
    SimpleDiffCallback { it.id }
) {

    fun setData(list: List<CrashEvent>) {
        submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrashEventViewHolder {
        val binding = ViewCrashEventRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CrashEventViewHolder(binding) { position ->
            if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                notifyItemClick(binding.root, position)
            }
        }
    }

    override fun onBindViewHolder(holder: CrashEventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CrashEventViewHolder(
    private val binding: ViewCrashEventRowBinding,
    onClick: (Int) -> Unit,
) : BaseListAdapter.BaseViewHolder<CrashEvent>(binding.root) {

    init {
        binding.root.setOnClickListener {
            onClick(bindingAdapterPosition)
        }
    }

    override fun bind(event: CrashEvent) {
        CrashEventBinder.bind(binding, event)
    }
}
