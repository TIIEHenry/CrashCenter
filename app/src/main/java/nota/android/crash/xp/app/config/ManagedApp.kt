package nota.android.crash.xp.app.config

import android.graphics.drawable.Drawable

data class ManagedApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystem: Boolean,
    val interventionStatus: InterventionStatus,
    val switchChecked: Boolean,
    val enabledRuleCount: Int,
    val summary: String?,
    val updateTime: Long = 0L,
    val installTime: Long = 0L,
)
