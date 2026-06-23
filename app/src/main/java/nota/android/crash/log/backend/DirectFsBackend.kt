package nota.android.crash.log.backend

import android.content.Context
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CanonicalJsonlWriter
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.ProcessSlot
import nota.android.crash.log.appendWithSafeWrite
import nota.android.crash.xp.PrefManager
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.FileCrashLogRepository
import java.io.File

object DirectFsBackend : CrashLogBackend {

    override val id = BackendId.DIRECT_FS
    override val tier = 2
    override val runsOn = ProcessSlot.HOOK

    override fun probe(context: Context): BackendAvailability {
        return try {
            val moduleContext = context.createPackageContext(
                PrefManager.PACKAGE_NAME,
                Context.CONTEXT_IGNORE_SECURITY,
            )
            val logDir = File(
                moduleContext.filesDir,
                FileCrashLogRepository.LOG_DIR,
            )
            if (logDir.exists() || logDir.mkdirs()) BackendAvailability.READY
            else BackendAvailability.UNAVAILABLE
        } catch (_: Throwable) {
            BackendAvailability.UNAVAILABLE
        }
    }

    override fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult {
        return appendWithSafeWrite(event) { stamped ->
            val moduleContext = context.createPackageContext(
                PrefManager.PACKAGE_NAME,
                Context.CONTEXT_IGNORE_SECURITY,
            )
            val eventsFile = File(
                moduleContext.filesDir,
                "${FileCrashLogRepository.LOG_DIR}/${FileCrashLogRepository.EVENTS_FILE}",
            )
            CanonicalJsonlWriter.append(eventsFile, stamped)
            AppendResult.Success
        }
    }
}
