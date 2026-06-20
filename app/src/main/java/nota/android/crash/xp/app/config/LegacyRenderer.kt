package nota.android.crash.xp.app.config

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

class LegacyRenderer(
    private val binding: FragmentConfigBinding,
    private val legacyAdapter: AppToggleAdapter,
) {
    fun render(state: ConfigUiState): Int {
        binding.recyclerv.adapter = legacyAdapter
        legacyAdapter.setData(state.visibleApps)
        val count = state.visibleApps.size
        FilterChipRow.setCountLabel(
            binding.hookFilterChipRow.root,
            binding.root.context.resources.getQuantityString(R.plurals.app_count, count, count),
        )
        return count
    }

    fun setVisibility(visible: Boolean) {
        binding.hookFilterChipRow.root.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
