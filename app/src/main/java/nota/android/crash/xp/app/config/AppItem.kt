package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo

data class AppItem(
    val label: String,
    val appInfo: ApplicationInfo,
    val hookEnabled: Boolean,
    val packageName: String,
    val isSystem: Boolean,
    val updateTime: Long,
    val installTime: Long,
)

enum class HookFilter {
    ALL, ON, OFF,
}

enum class SortMode {
    UPDATE_TIME_DESC,
    UPDATE_TIME_ASC,
    INSTALL_TIME_DESC,
    INSTALL_TIME_ASC,
    NAME_ASC,
    NAME_DESC,
}
