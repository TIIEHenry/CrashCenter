package nota.android.crash.xp.app.config

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

class ManagedRenderer(
    private val binding: FragmentConfigBinding,
    private val managedAdapter: ManagedAppAdapter,
) {
    fun render(state: ConfigUiState): Int {
        binding.recyclerv.adapter = managedAdapter
        managedAdapter.setData(state.visibleManagedApps)
        val count = state.visibleManagedApps.size
        FilterChipRow.setCountLabel(
            binding.managedFilterChipRow.root,
            binding.root.context.resources.getQuantityString(R.plurals.app_count, count, count),
        )
        return count
    }

    fun setVisibility(visible: Boolean) {
        binding.managedFilterChipRow.root.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
