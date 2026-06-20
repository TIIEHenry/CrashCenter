package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

class LegacyConfigController(
    binding: FragmentConfigBinding,
    onToggleApp: (String) -> Unit,
    onHookFilterChanged: (HookFilter) -> Unit,
) {
    private val adapter = AppToggleAdapter().apply {
        onItemClick { _, data, _ -> onToggleApp(data.packageName) }
    }

    private val renderer = LegacyRenderer(binding, adapter)

    init {
        FilterChipRow.setOnSingleSelectionChangeListener(
            binding.hookFilterChipRow.root,
            R.id.hook_chipGroup,
        ) { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.hook_chipOn -> HookFilter.ON
                R.id.hook_chipOff -> HookFilter.OFF
                else -> HookFilter.ALL
            }
            onHookFilterChanged(filter)
        }
    }

    fun render(state: ConfigUiState): Int = renderer.render(state)
    fun setVisibility(visible: Boolean) = renderer.setVisibility(visible)
}
