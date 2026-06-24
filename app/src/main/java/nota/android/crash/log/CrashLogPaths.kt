package nota.android.crash.log

import android.content.Context
import java.io.File

/** Path SSOT for distributed per-app cache JSONL (ADR-024). */
object CrashLogPaths {
    const val LOG_DIR = "crash_logs"
    const val EVENTS_FILE = "events.jsonl"
    const val RELAY_DIR_LEGACY = "crashcenter_relay"

    const val USER_BASE_PATH = "/data/user"
    const val MAX_USER_ID = 150
    const val MAX_SCAN_FILES = 500

    fun eventsFile(context: Context): File =
        File(context.applicationContext.cacheDir, "$LOG_DIR/$EVENTS_FILE")

    fun legacyCanonicalFile(context: Context): File =
        File(context.applicationContext.filesDir, "$LOG_DIR/$EVENTS_FILE")

    fun legacyRelayDir(userId: Int, packageName: String): String =
        "$USER_BASE_PATH/$userId/$packageName/files/$RELAY_DIR_LEGACY"

    fun eventsPath(userId: Int, packageName: String): String =
        "$USER_BASE_PATH/$userId/$packageName/cache/$LOG_DIR/$EVENTS_FILE"
}
