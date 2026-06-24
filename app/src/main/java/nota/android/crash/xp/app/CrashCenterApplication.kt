package nota.android.crash.xp.app

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nota.android.crash.log.CrashLogMigrationCoordinator
import nota.android.crash.root.AppShell

class CrashCenterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppShell.initMainShell()
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, t ->
                Log.w(TAG, "Background coroutine failed", t)
            },
        ).launch {
            CrashLogMigrationCoordinator.migrate(this@CrashCenterApplication)
        }
    }

    companion object {
        private const val TAG = "CrashCenterApp"
    }
}
