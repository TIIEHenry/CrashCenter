package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo

data class PickableApp(
    val packageName: String,
    val label: String,
    val appInfo: ApplicationInfo,
    val isSystem: Boolean,
)
