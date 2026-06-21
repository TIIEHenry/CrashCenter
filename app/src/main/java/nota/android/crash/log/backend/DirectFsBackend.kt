package nota.android.crash.log.backend

import android.content.Context
import de.robv.android.xposed.XposedBridge
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CanonicalJsonlWriter
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.ProcessSlot
import nota.android.crash.xp.PrefManager
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.FileCrashLogRepository
import java.io.File

object DirectFsBackend : CrashLogBackend {

    override val id = BackendId.DIRECT_FS
    override val tier = 2
    override val runsOn = ProcessSlot.HOOK

    override fun probe(context: Context): BackendAvailability = BackendAvailability.MAYBE

    override fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult {
        return try {
            val moduleContext = context.createPackageContext(
                PrefManager.PACKAGE_NAME,
                Context.CONTEXT_IGNORE_SECURITY,
            )
            val eventsFile = File(
                moduleContext.filesDir,
                "${FileCrashLogRepository.LOG_DIR}/${FileCrashLogRepository.EVENTS_FILE}",
            )
            val stamped = event.withBackendWritten(listOf(id.wireName))
            CanonicalJsonlWriter.append(eventsFile, stamped)
            AppendResult.Success
        } catch (t: Throwable) {
            XposedBridge.log("CrashLog direct_fs failed: ${t.message}")
            AppendResult.Failure(t.message ?: t.javaClass.simpleName)
        }
    }
}
