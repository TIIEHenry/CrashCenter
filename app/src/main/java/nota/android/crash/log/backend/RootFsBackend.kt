package nota.android.crash.log.backend

import android.content.Context
import kotlinx.coroutines.runBlocking
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.ProcessSlot
import nota.android.crash.log.appendWithSafeWrite
import nota.android.crash.root.RootAvailability
import nota.android.crash.xp.app.data.FileCrashLogRepository
import nota.android.crash.xp.app.di.ServiceLocator

/**
 * Module-side backend that uses [nota.android.crash.root.RootAccessClient] to write
 * crash events to the canonical JSONL path via root file system access.
 *
 * Runs on [ProcessSlot.MODULE]; used by CrashLogIngestCoordinator (4B-beta).
 * Follows the same canonical path as [DirectFsBackend] but operates through
 * the unified root service layer (see unified-root-service.md).
 */
object RootFsBackend : CrashLogBackend {

    override val id = BackendId.ROOT_FS
    override val tier = 0
    override val runsOn = ProcessSlot.MODULE

    override fun probe(context: Context): BackendAvailability {
        val client = ServiceLocator.rootAccessClient(context)
        return when (client.probe()) {
            RootAvailability.AVAILABLE -> BackendAvailability.READY
            RootAvailability.DENIED -> BackendAvailability.MAYBE
            RootAvailability.UNAVAILABLE -> BackendAvailability.UNAVAILABLE
        }
    }

    override fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult {
        return appendWithSafeWrite(event) { stamped ->
            val path = FileCrashLogRepository.eventsFile(context).absolutePath
            val line = stamped.toJsonLine() + "\n"
            val client = ServiceLocator.rootAccessClient(context)
            val success = runBlocking {
                client.appendBytes(path, line.toByteArray(Charsets.UTF_8), deadlineMs)
            }
            if (success) AppendResult.Success
            else AppendResult.Failure("root append failed")
        }
    }
}
