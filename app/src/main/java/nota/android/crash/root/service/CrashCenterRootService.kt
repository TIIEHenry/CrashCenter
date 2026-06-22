package nota.android.crash.root.service

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

class CrashCenterRootService : RootService() {
    override fun onBind(intent: Intent): IBinder = RootBroker()
}
