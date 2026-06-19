package nota.android.crash.xp.app.observe

import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.data.CrashEvent
import nota.android.crash.xp.app.databinding.ViewCrashEventRowBinding

class CrashHistoryAdapter : ListAdapter<CrashEvent, CrashHistoryAdapter.VH>(DiffCallback()) {

    private var onItemClickListener: ((View, CrashEvent, Int) -> Unit)? = null

    fun onItemClick(f: (rootView: View, data: CrashEvent, pos: Int) -> Unit) {
        onItemClickListener = f
    }

    fun setData(list: List<CrashEvent>) {
        submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ViewCrashEventRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ViewCrashEventRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(binding.root, getItem(pos), pos)
                }
            }
        }

        fun bind(event: CrashEvent) {
            val context = binding.root.context

            val label = event.appLabel?.takeIf { it.isNotEmpty() } ?: event.packageName
            binding.tvAppName.text = label

            val relativeTime = DateUtils.getRelativeTimeSpanString(
                event.timestampMs,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            )
            val detail = event.message?.takeIf { it.isNotBlank() } ?: event.packageName
            val subtitle = context.getString(
                R.string.crash_event_subtitle_format,
                event.shortExceptionClass,
                detail,
                relativeTime,
            )
            binding.root.contentDescription = context.getString(
                R.string.legacy_app_row_a11y,
                label,
                subtitle,
            )
            binding.tvSubtitle.text = subtitle

            binding.ivIcon.setImageDrawable(loadIcon(context, event.packageName))

            val sourceLabel = formatSource(context, event.source)
            if (sourceLabel != null) {
                binding.tvSourceBadge.visibility = View.VISIBLE
                binding.tvSourceBadge.text = sourceLabel.label
                binding.tvSourceBadge.contentDescription = sourceLabel.contentDescription
            } else {
                binding.tvSourceBadge.visibility = View.GONE
                binding.tvSourceBadge.contentDescription = null
            }
        }

        private fun loadIcon(context: Context, packageName: String) = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        }

        private fun formatSource(context: Context, source: String?): SourceLabel? = when (source?.lowercase()) {
            "uncaught" -> SourceLabel(
                context.getString(R.string.crash_source_ueh),
                context.getString(R.string.crash_source_ueh_a11y),
            )
            "looper" -> SourceLabel(
                context.getString(R.string.crash_source_looper),
                context.getString(R.string.crash_source_looper_a11y),
            )
            null, "" -> null
            else -> SourceLabel(source, source)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CrashEvent>() {
        override fun areItemsTheSame(oldItem: CrashEvent, newItem: CrashEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CrashEvent, newItem: CrashEvent): Boolean {
            return oldItem == newItem
        }
    }
}

private data class SourceLabel(val label: String, val contentDescription: String)
