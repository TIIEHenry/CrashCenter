package nota.android.crash.xp.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nota.android.crash.log.CrashLogIngestCoordinator
import nota.android.crash.root.AppShell

class CrashCenterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppShell.initMainShell()
        CoroutineScope(Dispatchers.IO).launch {
            CrashLogIngestCoordinator.ingest(this@CrashCenterApplication)
        }
    }
}
