package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

class LegacyRenderer(
    binding: FragmentConfigBinding,
    legacyAdapter: AppToggleAdapter,
) {
    private val renderer = AppListRenderer(
        recyclerView = binding.recyclerv,
        filterChipRowRoot = binding.hookFilterChipRow.root,
        countLabelId = R.id.hook_countLabel,
        adapter = legacyAdapter,
        dataSelector = { it.visibleApps },
    )

    fun render(state: ConfigUiState): Int = renderer.render(state)
    fun setVisibility(visible: Boolean) = renderer.setVisibility(visible)
}
