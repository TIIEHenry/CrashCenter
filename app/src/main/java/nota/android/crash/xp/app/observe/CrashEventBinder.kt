package nota.android.crash.xp.app.observe

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import androidx.core.content.ContextCompat
import nota.android.crash.xp.app.R
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.databinding.ViewCrashEventRowBinding
import nota.android.crash.xp.app.databinding.ViewPerAppCrashEventRowBinding

object CrashEventBinder {

    fun bindGlobal(binding: ViewCrashEventRowBinding, event: CrashEvent) {
        val context = binding.root.context

        val label = event.appLabel?.takeIf { it.isNotEmpty() } ?: event.packageName
        binding.tvAppName.text = label

        val relativeTime = relativeTime(context, event)
        binding.tvTimestamp.text = relativeTime

        val detail = event.message?.takeIf { it.isNotBlank() } ?: event.packageName
        val subtitle = context.getString(
            R.string.crash_event_subtitle_format,
            event.shortExceptionClass,
            detail,
        )
        binding.root.contentDescription = context.getString(
            R.string.legacy_app_row_a11y,
            label,
            subtitle,
            relativeTime,
        )
        binding.tvSubtitle.text = subtitle

        binding.ivIcon.setImageDrawable(loadIcon(context, event.packageName))

        bindSourceBadge(context, binding.tvSourceBadge, event.source)
    }

    fun bindPerApp(binding: ViewPerAppCrashEventRowBinding, event: CrashEvent) {
        val context = binding.root.context

        binding.tvExceptionClass.text = event.shortExceptionClass

        binding.tvTimestamp.text = relativeTime(context, event)

        val message = event.message?.takeIf { it.isNotBlank() }
        if (message != null) {
            binding.tvMessage.visibility = View.VISIBLE
            binding.tvMessage.text = message
        } else {
            binding.tvMessage.visibility = View.GONE
        }

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

    private fun relativeTime(context: Context, event: CrashEvent) = DateUtils.getRelativeTimeSpanString(
        event.timestampMs,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    )

    private fun bindSourceBadge(context: Context, badge: android.widget.TextView, source: String?) {
        val sourceLabel = formatSource(context, source)
        if (sourceLabel != null) {
            badge.visibility = View.VISIBLE
            badge.text = sourceLabel.label
            badge.contentDescription = sourceLabel.contentDescription
        } else {
            badge.visibility = View.GONE
            badge.contentDescription = null
        }
    }

    private fun loadIcon(context: Context, packageName: String) = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
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

    private data class SourceLabel(val label: String, val contentDescription: String)
}
