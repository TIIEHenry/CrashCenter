package nota.android.crash.xp.app.config

import android.view.View
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

data class EmptyDecision(
    val isEmpty: Boolean,
    val messageResId: Int,
    val showAddAction: Boolean,
    val iconResId: Int?,
)

fun decideEmpty(state: ConfigUiState, listCount: Int): EmptyDecision {
    val empty = !state.isLoading && listCount == 0
    if (!empty) return EmptyDecision(
        isEmpty = false,
        messageResId = R.string.managed_empty_message,
        showAddAction = false,
        iconResId = null,
    )
    val showAddAction = !state.isLegacyMode &&
        state.emptyMessage == ConfigViewModel.EMPTY_MANAGED_LIST
    val messageResId = when (state.emptyMessage) {
        ConfigViewModel.EMPTY_MANAGED_LIST -> R.string.managed_empty_message
        else -> if (state.isLegacyMode) R.string.filter_empty else R.string.managed_filter_empty
    }
    return EmptyDecision(
        isEmpty = true,
        messageResId = messageResId,
        showAddAction = showAddAction,
        iconResId = if (showAddAction) R.drawable.ic_tab_config else null,
    )
}

class EmptyStateRenderer(
    private val binding: FragmentConfigBinding,
    private val onAddManagedApp: () -> Unit,
) {
    fun render(state: ConfigUiState, listCount: Int) {
        val decision = decideEmpty(state, listCount)
        binding.emptyState.root.visibility = if (decision.isEmpty) View.VISIBLE else View.GONE
        binding.recyclerv.visibility = if (!decision.isEmpty && listCount > 0) View.VISIBLE else View.GONE

        if (decision.isEmpty) {
            val message = binding.root.context.getString(decision.messageResId)
            EmptyState.bind(
                binding.emptyState.root,
                message,
                if (decision.showAddAction) binding.root.context.getString(R.string.add_managed_app) else null,
                if (decision.showAddAction) ({ onAddManagedApp() }) else null,
                decision.iconResId,
            )
        }
    }
}
