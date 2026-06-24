package nota.android.crash.xp.app.observe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.databinding.ViewCrashEventRowBinding
import nota.android.crash.xp.app.databinding.ViewPerAppCrashEventRowBinding

enum class CrashHistoryDisplayMode { GLOBAL, PER_APP }

class CrashEventViewHolder(
    itemView: View,
    private val binder: (CrashEvent) -> Unit,
    onClick: (Int) -> Unit,
    onLongClick: ((Int) -> Boolean)? = null,
) : RecyclerView.ViewHolder(itemView) {

    init {
        itemView.setOnClickListener {
            onClick(bindingAdapterPosition)
        }
        if (onLongClick != null) {
            itemView.setOnLongClickListener {
                onLongClick(bindingAdapterPosition)
            }
        }
    }

    fun bind(event: CrashEvent) {
        binder(event)
    }
}

class CrashHistoryPagingAdapter(
    private val onItemClick: (CrashEvent) -> Unit,
    private val onItemLongClick: ((CrashEvent) -> Unit)? = null,
    private val displayMode: CrashHistoryDisplayMode = CrashHistoryDisplayMode.GLOBAL,
) : PagingDataAdapter<CrashEvent, CrashEventViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrashEventViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val longClick: ((Int) -> Boolean)? = onItemLongClick?.let { handler ->
            { position ->
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { event ->
                        handler(event)
                        true
                    } ?: false
                } else false
            }
        }
        return when (displayMode) {
            CrashHistoryDisplayMode.GLOBAL -> {
                val binding = ViewCrashEventRowBinding.inflate(inflater, parent, false)
                CrashEventViewHolder(binding.root,
                    binder = { event -> CrashEventBinder.bindGlobal(binding, event) },
                    onClick = { position ->
                        if (position != RecyclerView.NO_POSITION) {
                            getItem(position)?.let { onItemClick(it) }
                        }
                    },
                    onLongClick = longClick,
                )
            }
            CrashHistoryDisplayMode.PER_APP -> {
                val binding = ViewPerAppCrashEventRowBinding.inflate(inflater, parent, false)
                CrashEventViewHolder(binding.root,
                    binder = { event -> CrashEventBinder.bindPerApp(binding, event) },
                    onClick = { position ->
                        if (position != RecyclerView.NO_POSITION) {
                            getItem(position)?.let { onItemClick(it) }
                        }
                    },
                    onLongClick = longClick,
                )
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