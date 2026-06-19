package nota.android.crash.xp.app.data

import android.content.Context

object CrashDetailLoader {

    @JvmStatic
    fun loadStackTraceById(context: Context, crashId: String): String? {
        if (crashId.isBlank()) return null
        val event = FileCrashLogRepository(context).getById(crashId) ?: return null
        return event.stackTrace.ifBlank { formatFallback(event) }
    }

    private fun formatFallback(event: CrashEvent): String {
        val header = buildString {
            append(event.exceptionClass)
            event.message?.let { append(": ").append(it) }
            append('\n')
            append("package=").append(event.packageName)
            append(" id=").append(event.id)
        }
        return header
    }
}
