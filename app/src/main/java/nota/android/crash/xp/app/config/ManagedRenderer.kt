package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

class ManagedRenderer(
    binding: FragmentConfigBinding,
    managedAdapter: ManagedAppAdapter,
) {
    private val renderer = AppListRenderer(
        recyclerView = binding.recyclerv,
        filterChipRowRoot = binding.managedFilterChipRow.root,
        countLabelId = R.id.managed_countLabel,
        adapter = managedAdapter,
        dataSelector = { it.visibleManagedApps },
    )

    fun render(state: ConfigUiState): Int = renderer.render(state)
    fun setVisibility(visible: Boolean) = renderer.setVisibility(visible)
}
