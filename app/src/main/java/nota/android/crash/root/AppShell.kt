package nota.android.crash.root

import com.topjohnwu.superuser.Shell

object AppShell {
    fun initMainShell() {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setCommands("su")
                .setTimeout(30)
        )
    }
}
