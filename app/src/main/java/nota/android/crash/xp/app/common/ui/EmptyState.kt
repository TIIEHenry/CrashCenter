package nota.android.crash.xp.app.common.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.google.android.material.button.MaterialButton
import nota.android.crash.xp.app.R

object EmptyState {

    @JvmStatic
    fun bind(root: View, message: CharSequence) {
        bind(root, message, actionLabel = null, onAction = null, iconRes = null)
    }

    @JvmStatic
    fun bind(root: View, message: CharSequence, @DrawableRes iconRes: Int?) {
        bind(root, message, actionLabel = null, onAction = null, iconRes = iconRes)
    }

    @JvmStatic
    fun bind(
        root: View,
        message: CharSequence,
        actionLabel: CharSequence?,
        onAction: (() -> Unit)?,
    ) {
        bind(root, message, actionLabel, onAction, iconRes = null)
    }

    @JvmStatic
    fun bind(
        root: View,
        message: CharSequence,
        actionLabel: CharSequence?,
        onAction: (() -> Unit)?,
        @DrawableRes iconRes: Int?,
    ) {
        when (root) {
            is TextView -> root.text = message
            else -> root.findViewById<TextView>(R.id.emptyMessage)?.text = message
        }
        bindIcon(root, iconRes)
        val actionButton = root.findViewById<MaterialButton>(R.id.emptyAction)
        if (actionButton == null || actionLabel == null || onAction == null) {
            actionButton?.visibility = View.GONE
            return
        }
        actionButton.visibility = View.VISIBLE
        actionButton.text = actionLabel
        actionButton.setOnClickListener { onAction() }
    }

    private fun bindIcon(root: View, @DrawableRes iconRes: Int?) {
        val icon = root.findViewById<ImageView>(R.id.emptyIcon) ?: return
        if (iconRes == null) {
            icon.visibility = View.GONE
            return
        }
        icon.visibility = View.VISIBLE
        icon.setImageResource(iconRes)
    }
}
