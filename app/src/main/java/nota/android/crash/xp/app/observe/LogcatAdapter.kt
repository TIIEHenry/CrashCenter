package nota.android.crash.xp.app.observe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.ItemLogcatEntryBinding

class LogcatAdapter(
    private val onItemClick: (LogcatEntry) -> Unit,
) : ListAdapter<LogcatEntry, LogcatAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogcatEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return ViewHolder(binding) { position ->
            if (position != RecyclerView.NO_POSITION) {
                onItemClick(getItem(position))
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
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
            binding.tvMessage.text = entry.displaySummary
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
