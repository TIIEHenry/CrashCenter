package nota.android.crash.xp.app.common.ui

import android.view.View
import nota.android.crash.xp.app.databinding.ViewLoadingStateBinding

object LoadingState {

    fun bind(root: View, message: CharSequence? = null) {
        val binding = ViewLoadingStateBinding.bind(root)
        if (message != null) {
            binding.loadingMessage.text = message
        }
    }
}
