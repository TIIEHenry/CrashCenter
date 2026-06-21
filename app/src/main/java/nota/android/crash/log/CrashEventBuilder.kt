package nota.android.crash.log

import android.content.Context
import android.util.Log
import nota.android.crash.common.data.CrashEvent
import java.util.UUID

object CrashEventBuilder {

    private const val MAX_STACK_CHARS = 64 * 1024

    fun build(
        packageName: String,
        appLabel: String?,
        processName: String?,
        throwable: Throwable,
        source: String,
    ): CrashEvent {
        val root = throwable.cause ?: throwable
        return CrashEvent(
            id = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            packageName = packageName,
            appLabel = appLabel,
            processName = processName,
            exceptionClass = root.javaClass.name,
            message = root.message,
            stackTrace = truncateStack(Log.getStackTraceString(throwable)),
            source = source,
        )
    }

    fun resolveProcessName(context: Context, fallbackPackage: String): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val name = android.app.Application.getProcessName()
            if (!name.isNullOrEmpty()) return name
        }
        return fallbackPackage
    }

    private fun truncateStack(stack: String): String {
        if (stack.length <= MAX_STACK_CHARS) return stack
        return stack.substring(0, MAX_STACK_CHARS) + "\n... [truncated]"
    }
}
