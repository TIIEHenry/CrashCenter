package nota.android.crash.xp.app.data

import android.content.Context

object CrashDetailLoader {

    fun loadStackTraceById(context: Context, crashId: String): String? {
        if (crashId.isBlank()) return null
        return try {
            val event = FileCrashLogRepository(context).getById(crashId) ?: return null
            event.stackTrace.ifBlank { formatFallback(event) }
        } catch (_: Exception) {
            null
        }
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
