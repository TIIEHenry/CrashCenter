package nota.android.crash.log

import android.content.Context
import nota.android.crash.xp.app.data.CrashEvent

enum class BackendId(val wireName: String) {
    ROOT_SU("root_su"),
    PROVIDER_INSERT("provider_insert"),
    DIRECT_FS("direct_fs"),
    TARGET_RELAY("target_relay"),
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
