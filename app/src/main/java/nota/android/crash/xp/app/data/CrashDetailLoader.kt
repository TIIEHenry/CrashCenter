package nota.android.crash.xp.app.data

import nota.android.crash.common.data.CrashEvent

object CrashDetailLoader {

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

    fun titleFromStackTrace(stackTrace: String): String? {
        val firstLine = stackTrace.lineSequence().firstOrNull()?.trim().orEmpty()
        if (firstLine.isEmpty()) return null
        val exceptionToken = firstLine.substringBefore(':').trim()
        return exceptionToken.substringAfterLast('.').ifBlank { exceptionToken }
    }
}
