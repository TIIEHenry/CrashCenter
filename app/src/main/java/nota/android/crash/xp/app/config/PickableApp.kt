package nota.android.crash.xp.app.config

import android.graphics.drawable.Drawable

data class PickableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystem: Boolean,
)
