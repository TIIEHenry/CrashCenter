package nota.android.crash.xp.app.observe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.databinding.ViewCrashEventRowBinding

class CrashEventViewHolder(
    private val binding: ViewCrashEventRowBinding,
    onClick: (Int) -> Unit,
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            onClick(bindingAdapterPosition)
        }
    }

    fun bind(event: CrashEvent) {
        CrashEventBinder.bind(binding, event)
    }
}

class CrashHistoryPagingAdapter(
    private val onItemClick: (CrashEvent) -> Unit,
) : PagingDataAdapter<CrashEvent, CrashEventViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrashEventViewHolder {
        val binding = ViewCrashEventRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CrashEventViewHolder(binding) { position ->
            if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                getItem(position)?.let { onItemClick(it) }
            }
        }
    }

    override fun onBindViewHolder(holder: CrashEventViewHolder, position: Int) {
        getItem(position)?.let { event ->
            holder.bind(event)
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CrashEvent>() {
            override fun areItemsTheSame(oldItem: CrashEvent, newItem: CrashEvent): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CrashEvent, newItem: CrashEvent): Boolean {
                return oldItem == newItem
            }
        }
    }
}
