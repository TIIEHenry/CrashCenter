package nota.android.crash.xp.app.config

import android.view.View
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

private fun decideEmpty(state: ConfigUiState, listCount: Int): Pair<Int, Boolean> {
    val empty = !state.isLoading && listCount == 0
    if (!empty) return Pair(R.string.managed_empty_message, false)
    val showAction = state.emptyMessage == ConfigViewModel.EMPTY_LIST
    val messageResId = when (state.emptyMessage) {
        ConfigViewModel.EMPTY_LIST -> R.string.managed_empty_message
        else -> R.string.filter_empty
    }
    return Pair(messageResId, showAction)
}

class EmptyStateRenderer(
    private val binding: FragmentConfigBinding,
    private val onAddManagedApp: () -> Unit,
) {
    fun render(state: ConfigUiState, listCount: Int) {
        val (messageResId, showAction) = decideEmpty(state, listCount)
        val isEmpty = !state.isLoading && listCount == 0

        binding.emptyState.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerv.visibility = if (!isEmpty && listCount > 0) View.VISIBLE else View.GONE

        if (isEmpty) {
            val message = binding.root.context.getString(messageResId)
            EmptyState.bind(
                binding.emptyState.root,
                message,
                null,
                null,
                null,
            )
        }
    }
}
