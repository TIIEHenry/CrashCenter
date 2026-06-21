package nota.android.crash.log.backend

import android.content.Context
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.ProcessSlot
import nota.android.crash.log.appendWithSafeWrite
import nota.android.crash.common.data.CrashEvent
import java.io.File

object TargetRelayBackend : CrashLogBackend {

    const val RELAY_DIR = "crashcenter_relay"

    override val id = BackendId.TARGET_RELAY
    override val tier = 3
    override val runsOn = ProcessSlot.HOOK

    override fun probe(context: Context): BackendAvailability = BackendAvailability.READY

    override fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult {
        return appendWithSafeWrite(event) { stamped ->
            val relayDir = File(context.filesDir, RELAY_DIR)
            if (!relayDir.exists() && !relayDir.mkdirs()) {
                return@appendWithSafeWrite AppendResult.Failure("mkdir relay failed")
            }
            val relayFile = File(relayDir, "${event.id}.json")
            relayFile.writeText(stamped.toJsonLine(), Charsets.UTF_8)
            AppendResult.Success
        }
    }
}
