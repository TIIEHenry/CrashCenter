package nota.android.crash.xp.app.observe

import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateUtils
import android.view.View
import androidx.core.content.ContextCompat
import nota.android.crash.xp.app.R
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.databinding.ViewCrashEventRowBinding

object CrashEventBinder {

    fun bind(binding: ViewCrashEventRowBinding, event: CrashEvent) {
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

    private data class SourceLabel(val label: String, val contentDescription: String)
}
