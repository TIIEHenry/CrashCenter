package nota.android.crash.xp.app

import android.app.Application
import nota.android.crash.root.AppShell

class CrashCenterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppShell.initMainShell()
    }
}
