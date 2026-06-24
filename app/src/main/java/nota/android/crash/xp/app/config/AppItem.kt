package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo

data class AppItem(
    override val packageName: String,
    override val label: String,
    override val appInfo: ApplicationInfo,
    override val isSystem: Boolean,
    val interceptEnabled: Boolean,
    override val updateTime: Long,
    override val installTime: Long,
) : AppListItem

enum class SortMode {
    UPDATE_TIME_DESC,
    UPDATE_TIME_ASC,
    INSTALL_TIME_DESC,
    INSTALL_TIME_ASC,
    NAME_ASC,
    NAME_DESC,
}
