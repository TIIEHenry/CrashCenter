package nota.android.crash.xp.app.common.ui

import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.data.CrashEvent

object CrashEventRow {

    @JvmStatic
    fun bind(root: View, event: CrashEvent, context: Context) {
        val appNameView = root.findViewById<TextView>(R.id.tvAppName)
        val subtitleView = root.findViewById<TextView>(R.id.tvSubtitle)
        val iconView = root.findViewById<ImageView>(R.id.ivIcon)
        val sourceBadge = root.findViewById<TextView>(R.id.tvSourceBadge)

        val label = event.appLabel?.takeIf { it.isNotEmpty() } ?: event.packageName
        appNameView.text = label

        val relativeTime = DateUtils.getRelativeTimeSpanString(
            event.timestampMs,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        )
        root.contentDescription = context.getString(
            R.string.crash_event_subtitle_format,
            event.shortExceptionClass,
            event.packageName,
            relativeTime,
        )
        subtitleView.text = context.getString(
            R.string.crash_event_subtitle_format,
            event.shortExceptionClass,
            event.packageName,
            relativeTime,
        )

        iconView.setImageDrawable(loadIcon(context, event.packageName))

        val sourceLabel = formatSource(context, event.source)
        if (sourceLabel != null) {
            sourceBadge.visibility = View.VISIBLE
            sourceBadge.text = sourceLabel
        } else {
            sourceBadge.visibility = View.GONE
        }
    }

    private fun loadIcon(context: Context, packageName: String) = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
    }

    private fun formatSource(context: Context, source: String?): String? = when (source?.lowercase()) {
        "uncaught" -> context.getString(R.string.crash_source_ueh)
        "looper" -> context.getString(R.string.crash_source_looper)
        null, "" -> null
        else -> source
    }
}
