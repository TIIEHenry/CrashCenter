package nota.android.crash.xp.app.common.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.google.android.material.button.MaterialButton
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.ViewEmptyStateActionBinding
import nota.android.crash.xp.app.databinding.ViewEmptyStateBinding

object EmptyState {

    fun bind(root: View, message: CharSequence) {
        bind(root, message, actionLabel = null, onAction = null, iconRes = null)
    }

    fun bind(root: View, message: CharSequence, @DrawableRes iconRes: Int?) {
        bind(root, message, actionLabel = null, onAction = null, iconRes = iconRes)
    }

    fun bind(
        root: View,
        message: CharSequence,
        actionLabel: CharSequence?,
        onAction: (() -> Unit)?,
    ) {
        bind(root, message, actionLabel, onAction, iconRes = null)
    }

    fun bind(
        root: View,
        message: CharSequence,
        actionLabel: CharSequence?,
        onAction: (() -> Unit)?,
        @DrawableRes iconRes: Int?,
    ) {
        when (root) {
            is TextView -> root.text = message
            else -> {
                val actionBinding = ViewEmptyStateActionBinding.bind(root)
                actionBinding.emptyMessage.text = message
                bindIcon(actionBinding.emptyIcon, iconRes)
                bindAction(actionBinding.emptyAction, actionLabel, onAction)
            }
        }
    }

    private fun bindIcon(icon: ImageView, @DrawableRes iconRes: Int?) {
        if (iconRes == null) {
            icon.visibility = View.GONE
            return
        }
        icon.visibility = View.VISIBLE
        icon.setImageResource(iconRes)
    }

    private fun bindAction(
        actionButton: MaterialButton,
        actionLabel: CharSequence?,
        onAction: (() -> Unit)?,
    ) {
        if (actionLabel == null || onAction == null) {
            actionButton.visibility = View.GONE
            return
        }
        actionButton.visibility = View.VISIBLE
        actionButton.text = actionLabel
        actionButton.setOnClickListener { onAction() }
    }
}
