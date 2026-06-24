package nota.android.crash.log.backend

import android.content.Context
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.CrashLogJsonlStore
import nota.android.crash.log.CrashLogPaths
import nota.android.crash.log.CrashLogRetention
import nota.android.crash.log.ProcessSlot
import nota.android.crash.log.appendWithSafeWrite

/** Hook-side write to this app's cache JSONL (ADR-024). */
object LocalCacheBackend : CrashLogBackend {

    override val id = BackendId.LOCAL_CACHE
    override val tier = 0
    override val runsOn = ProcessSlot.HOOK

    override fun probe(context: Context): BackendAvailability = BackendAvailability.READY

    override fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult {
        return appendWithSafeWrite(event) { stamped ->
            val eventsFile = CrashLogPaths.eventsFile(context)
            val limits = CrashLogRetention.fromXSharedPreferences()
            CrashLogJsonlStore.append(
                eventsFile,
                stamped,
                limits.maxEntries,
                limits.maxBytes,
            )
            AppendResult.Success
        }
    }
}
