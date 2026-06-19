package nota.android.crash.log

import android.net.Uri

object CrashLogContract {
    const val AUTHORITY = "nota.android.crash.xp.app.crashlog"
    const val PATH_EVENTS = "events"

    const val COLUMN_PAYLOAD = "payload"
    const val COLUMN_PACKAGE_NAME = "package_name"

    val EVENTS_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_EVENTS")
}
