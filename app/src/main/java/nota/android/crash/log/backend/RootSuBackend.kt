package nota.android.crash.log.backend

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.runBlocking
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.ProcessSlot
import nota.android.crash.log.appendWithSafeWrite
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import nota.android.crash.root.ShellOnlyAdapter
import nota.android.crash.xp.app.data.FileCrashLogRepository

/**
 * Hook-process Phase 1 backend that appends crash events to the canonical
 * JSONL via `su -c` using [ShellOnlyAdapter].
 *
 * Tier 0 (highest priority); runs only in [ProcessSlot.HOOK].
 * Uses a 1.5-second deadline per ADR-008.
 * Silent failure on all errors — falls through to Phase 2 backends.
 *
 * This backend does NOT use ServiceLocator (hook process lacks it) and
 * does NOT call RootService.bind() (hook process must never bind).
 */
object RootSuBackend : CrashLogBackend {

    private const val DEADLINE_MS = 1500L

    override val id = BackendId.ROOT_SU
    override val tier = 0
    override val runsOn = ProcessSlot.HOOK

    @JvmStatic
    @VisibleForTesting
    internal var adapter: RootAccessClient = ShellOnlyAdapter()

    override fun probe(context: Context): BackendAvailability {
        return try {
            when (adapter.probe()) {
                RootAvailability.AVAILABLE -> BackendAvailability.READY
                RootAvailability.DENIED -> BackendAvailability.MAYBE
                RootAvailability.UNAVAILABLE -> BackendAvailability.UNAVAILABLE
            }
        } catch (_: Exception) {
            BackendAvailability.UNAVAILABLE
        }
    }

    override fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult {
        return appendWithSafeWrite(event) { stamped ->
            val path = FileCrashLogRepository.eventsFile(context).absolutePath
            val line = stamped.toJsonLine() + "\n"
            val success = runBlocking {
                adapter.appendBytes(path, line.toByteArray(Charsets.UTF_8), DEADLINE_MS)
            }
            if (success) AppendResult.Success
            else AppendResult.Failure("su append failed")
        }
    }
}
