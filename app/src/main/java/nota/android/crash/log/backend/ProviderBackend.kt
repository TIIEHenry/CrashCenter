package nota.android.crash.log.backend

import android.content.ContentValues
import android.content.Context
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.CrashLogContract
import nota.android.crash.log.ProcessSlot
import nota.android.crash.log.appendWithSafeWrite
import nota.android.crash.common.data.CrashEvent

object ProviderBackend : CrashLogBackend {

    override val id = BackendId.PROVIDER_INSERT
    override val tier = 1
    override val runsOn = ProcessSlot.HOOK

    override fun probe(context: Context): BackendAvailability = BackendAvailability.READY

    override fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult {
        return appendWithSafeWrite(event) { stamped ->
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
        }
    }
}
