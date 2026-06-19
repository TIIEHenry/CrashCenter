package nota.android.crash.log

import android.content.Context
import de.robv.android.xposed.XposedBridge
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.data.CrashEvent
import nota.android.crash.xp.app.data.FileCrashLogRepository
import java.io.File
import java.io.FileOutputStream

/**
 * MVP scaffold: append one JSONL line to the module canonical events file via
 * [Context.createPackageContext] (hook process → module filesDir).
 */
object DirectFsCrashLogWriter {

    fun append(hookContext: Context, event: CrashEvent) {
        val moduleContext = hookContext.createPackageContext(
            PrefManager.PACKAGE_NAME,
            Context.CONTEXT_IGNORE_SECURITY,
        )
        val logDir = File(moduleContext.filesDir, FileCrashLogRepository.LOG_DIR)
        if (!logDir.exists() && !logDir.mkdirs()) {
            return
        }
        val eventsFile = File(logDir, FileCrashLogRepository.EVENTS_FILE)
        FileOutputStream(eventsFile, true).use { out ->
            out.write(event.toJsonLine().toByteArray(Charsets.UTF_8))
            out.write('\n'.code)
        }
    }

    fun appendSilent(hookContext: Context, event: CrashEvent) {
        try {
            append(hookContext, event)
        } catch (t: Throwable) {
            XposedBridge.log("CrashLog write failed: ${t.message}")
        }
    }
}
