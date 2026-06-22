package nota.android.crash.root

import com.topjohnwu.superuser.Shell

object AppShell {
    fun initMainShell() {
        try {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setCommands("su")
                    .setTimeout(30)
            )
        } catch (_: IllegalStateException) {
            // Already initialized (e.g. multiple Application.onCreate calls or Robolectric)
        }
    }
}
