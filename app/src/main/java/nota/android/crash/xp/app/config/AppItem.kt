package nota.android.crash.xp.app.config

import android.graphics.drawable.Drawable

data class AppItem(
    val name: String,
    val icon: Drawable,
    var hookEnabled: Boolean,
    val packageName: String,
    val isSystemApp: Boolean,
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
