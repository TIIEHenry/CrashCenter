package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo

data class ManagedApp(
    override val packageName: String,
    override val label: String,
    override val appInfo: ApplicationInfo,
    override val isSystem: Boolean,
    val interventionStatus: InterventionStatus,
    val switchChecked: Boolean,
    val enabledRuleCount: Int,
    val summary: String?,
    override val updateTime: Long = 0L,
    override val installTime: Long = 0L,
) : AppListItem
