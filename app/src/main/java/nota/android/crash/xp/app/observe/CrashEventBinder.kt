package nota.android.crash.xp.app.observe

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
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
        val outcomeLabel = outcomeLabel(context, event)
        binding.root.contentDescription = buildString {
            append(label)
            append("，")
            append(outcomeLabel)
            append("，")
            append(subtitle)
            append("，")
            append(relativeTime)
        }
        binding.tvSubtitle.text = subtitle

        binding.ivIcon.setImageDrawable(loadIcon(context, event.packageName))

        bindOutcomeBadge(context, binding.tvOutcomeBadge, event)
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

        bindOutcomeBadge(context, binding.tvOutcomeBadge, event)
        bindSourceBadge(context, binding.tvSourceBadge, event.source)
    }

    private fun relativeTime(context: Context, event: CrashEvent) = DateUtils.getRelativeTimeSpanString(
        event.timestampMs,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    )

    private fun bindOutcomeBadge(context: Context, badge: TextView, event: CrashEvent) {
        if (event.intercepted) {
            badge.visibility = View.VISIBLE
            badge.setText(R.string.crash_outcome_intercepted)
            badge.setBackgroundResource(R.drawable.bg_status_active)
            badge.setTextColor(ContextCompat.getColor(context, R.color.statusActiveText))
            badge.contentDescription = context.getString(R.string.crash_outcome_intercepted_a11y)
        } else {
            badge.visibility = View.VISIBLE
            badge.setText(R.string.crash_outcome_monitor_only)
            badge.setBackgroundResource(R.drawable.bg_status_inactive)
            badge.setTextColor(ContextCompat.getColor(context, R.color.statusInactiveText))
            badge.contentDescription = context.getString(R.string.crash_outcome_monitor_only_a11y)
        }
    }

    private fun bindSourceBadge(context: Context, badge: TextView, source: String?) {
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

    private fun outcomeLabel(context: Context, event: CrashEvent): String =
        if (event.intercepted) {
            context.getString(R.string.crash_outcome_intercepted)
        } else {
            context.getString(R.string.crash_outcome_monitor_only)
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
