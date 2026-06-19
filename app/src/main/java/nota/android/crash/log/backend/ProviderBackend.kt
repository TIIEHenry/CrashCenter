package nota.android.crash.log.backend

import android.content.ContentValues
import android.content.Context
import de.robv.android.xposed.XposedBridge
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.CrashLogContract
import nota.android.crash.log.ProcessSlot
import nota.android.crash.xp.app.data.CrashEvent

object ProviderBackend : CrashLogBackend {

    override val id = BackendId.PROVIDER_INSERT
    override val tier = 1
    override val runsOn = ProcessSlot.HOOK

    override fun probe(context: Context): BackendAvailability = BackendAvailability.READY

    override fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult {
        return try {
            val stamped = event.withBackendWritten(listOf(id.wireName))
            val values = ContentValues().apply {
                put(CrashLogContract.COLUMN_PAYLOAD, stamped.toJsonLine())
                put(CrashLogContract.COLUMN_PACKAGE_NAME, event.packageName)
            }
            val uri = context.contentResolver.insert(CrashLogContract.EVENTS_URI, values)
            if (uri != null) {
                AppendResult.Success
            } else {
                AppendResult.Failure("insert returned null")
            }
        } catch (t: Throwable) {
            XposedBridge.log("CrashLog provider_insert failed: ${t.message}")
            AppendResult.Failure(t.message ?: t.javaClass.simpleName)
        }
    }
}
