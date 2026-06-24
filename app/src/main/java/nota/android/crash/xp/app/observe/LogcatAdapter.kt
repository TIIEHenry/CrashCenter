package nota.android.crash.xp.app.observe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.ItemLogcatEntryBinding

class LogcatAdapter(
    private val onItemClick: (LogcatEntry) -> Unit,
) : PagingDataAdapter<LogcatEntry, LogcatAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogcatEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return ViewHolder(binding) { position ->
            if (position != RecyclerView.NO_POSITION) {
                getItem(position)?.let(onItemClick)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    class ViewHolder(
        private val binding: ItemLogcatEntryBinding,
        onClick: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { onClick(bindingAdapterPosition) }
        }

        fun bind(entry: LogcatEntry) {
            binding.tvTimestamp.text = entry.timestamp
            binding.tvLevel.text = entry.level.label
            binding.tvLevel.setTextColor(levelColor(entry.level))
            binding.tvTag.text = entry.tag
            val summary = entry.displaySummary
            binding.tvMessage.text = if (LogcatParser.isAnrHint(entry)) {
                "ANR · $summary"
            } else {
                summary
            }
        }

        private fun levelColor(level: LogcatLevel): Int {
            val resId = when (level) {
                LogcatLevel.FATAL -> R.color.logcat_fatal
                LogcatLevel.ERROR -> R.color.logcat_error
                LogcatLevel.WARNING -> R.color.logcat_warning
                LogcatLevel.INFO -> R.color.logcat_info
                LogcatLevel.DEBUG -> R.color.logcat_debug
                LogcatLevel.VERBOSE -> R.color.logcat_verbose
                LogcatLevel.SILENT -> R.color.logcat_debug
            }
            return ContextCompat.getColor(itemView.context, resId)
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LogcatEntry>() {
            override fun areItemsTheSame(oldItem: LogcatEntry, newItem: LogcatEntry): Boolean {
                return oldItem.timestamp == newItem.timestamp
                    && oldItem.pid == newItem.pid
                    && oldItem.tid == newItem.tid
                    && oldItem.tag == newItem.tag
            }

            override fun areContentsTheSame(oldItem: LogcatEntry, newItem: LogcatEntry): Boolean {
                return oldItem == newItem
            }
        }
    }
}
