package nota.android.crash.xp.app.config

import android.view.View
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

class EmptyStateRenderer(
    private val binding: FragmentConfigBinding,
    private val onAddManagedApp: () -> Unit,
) {
    fun render(state: ConfigUiState, listCount: Int) {
        val empty = !state.isLoading && listCount == 0
        binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerv.visibility = if (!state.isLoading && listCount > 0) View.VISIBLE else View.GONE

        if (empty) {
            val message = when (state.emptyMessage) {
                ConfigViewModel.EMPTY_MANAGED_LIST -> binding.root.context.getString(R.string.managed_empty_message)
                else -> binding.root.context.getString(
                    if (state.isLegacyMode) R.string.filter_empty else R.string.managed_filter_empty,
                )
            }
            val showAddAction = !state.isLegacyMode &&
                state.emptyMessage == ConfigViewModel.EMPTY_MANAGED_LIST
            EmptyState.bind(
                binding.emptyState.root,
                message,
                if (showAddAction) binding.root.context.getString(R.string.add_managed_app) else null,
                if (showAddAction) ({ onAddManagedApp() }) else null,
                if (showAddAction) R.drawable.ic_tab_config else null,
            )
        }
    }
}
