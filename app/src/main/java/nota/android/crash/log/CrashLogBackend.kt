package nota.android.crash.log

import android.content.Context
import de.robv.android.xposed.XposedBridge
import nota.android.crash.common.data.CrashEvent

enum class BackendId(val wireName: String) {
    LOCAL_CACHE("local_cache"),
    ROOT_FS("root_fsm"),
}

enum class BackendAvailability {
    READY,
    MAYBE,
    UNAVAILABLE,
}

enum class ProcessSlot {
    HOOK,
    MODULE,
}

sealed class AppendResult {
    data object Success : AppendResult()
    data class Failure(val reason: String) : AppendResult()
}

/**
 * Unified crash log write backend (ADR-008).
 * Implementations must catch internally; [append] must not throw.
 */
interface CrashLogBackend {
    val id: BackendId
    val tier: Int
    val runsOn: ProcessSlot

    fun probe(context: Context): BackendAvailability

    fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult
}

/**
 * Common boilerplate for [CrashLogBackend.append] implementations.
 * Stamps the event with this backend's id, then delegates to [write] inside a try-catch.
 * Exceptions are logged via [XposedBridge.log] and converted to [AppendResult.Failure].
 */
internal fun CrashLogBackend.appendWithSafeWrite(
    event: CrashEvent,
    write: (stamped: CrashEvent) -> AppendResult,
): AppendResult {
    return try {
        val stamped = event.withBackendWritten(listOf(id.wireName))
        write(stamped)
    } catch (t: Throwable) {
        XposedBridge.log("CrashLog ${id.wireName} failed: ${t.message}")
        AppendResult.Failure(t.message ?: t.javaClass.simpleName)
    }
}
