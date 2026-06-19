package taokdao.codeeditor.base

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.core.content.ContextCompat

fun Context.getDrawableCompact(@androidx.annotation.DrawableRes id: Int): Drawable? {
    return ContextCompat.getDrawable(this, id)
}

fun Context.toast(@androidx.annotation.StringRes id: Int) {
    Toast.makeText(this, id, Toast.LENGTH_SHORT).show()
}


fun Context.toast(s: String) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
