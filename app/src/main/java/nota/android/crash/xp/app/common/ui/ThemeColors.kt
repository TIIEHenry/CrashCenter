package nota.android.crash.xp.app.common.ui

import android.content.Context
import androidx.annotation.AttrRes
import com.google.android.material.color.MaterialColors

internal fun Context.themeColor(@AttrRes attr: Int): Int =
    MaterialColors.getColor(this, attr, "CrashCenter")
