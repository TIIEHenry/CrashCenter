package nota.android.crash.xp.app.common.ui

import android.content.Context
import android.widget.Toast

fun Context.showErrorToast(errorMessage: String?, clearError: (() -> Unit)? = null) {
    errorMessage ?: return
    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    clearError?.invoke()
}
