package nota.android.crash.log.backend

import android.content.Context
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.ProcessSlot
import nota.android.crash.root.RootAvailability
import nota.android.crash.xp.app.di.ServiceLocator

/** Module-side root availability probe (ADR-024). */
object RootFsBackend : CrashLogBackend {

    override val id = BackendId.ROOT_FS
    override val tier = 0
    override val runsOn = ProcessSlot.MODULE

    override fun probe(context: Context): BackendAvailability {
        return when (ServiceLocator.rootAccessClient(context).probe()) {
            RootAvailability.AVAILABLE -> BackendAvailability.READY
            RootAvailability.DENIED -> BackendAvailability.MAYBE
            RootAvailability.UNAVAILABLE -> BackendAvailability.UNAVAILABLE
        }
    }

    override fun append(
        context: Context,
        event: nota.android.crash.common.data.CrashEvent,
        deadlineMs: Long,
    ) = nota.android.crash.log.AppendResult.Failure("root fs read-only in ADR-024")
}
