package nota.android.crash.xp.app.data

object CrashDetailLoader {

    fun loadStackTraceById(repository: CrashLogRepository, crashId: String): String? {
        if (crashId.isBlank()) return null
        return try {
            val event = repository.getById(crashId) ?: return null
            stackTraceFrom(event)
        } catch (_: Exception) {
            null
        }
    }

    fun stackTraceFrom(event: CrashEvent): String =
        event.stackTrace.ifBlank { formatFallback(event) }

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
