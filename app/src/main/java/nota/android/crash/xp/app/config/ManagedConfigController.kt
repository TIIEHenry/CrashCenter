package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

class ManagedConfigController(
    binding: FragmentConfigBinding,
    onSwitchChanged: (ManagedApp, Boolean) -> Unit,
    onItemClick: (ManagedApp) -> Unit,
    onManagedFilterChanged: (ManagedFilter) -> Unit,
) {
    private val adapter = ManagedAppAdapter().apply {
        this.onSwitchChanged = onSwitchChanged
        onItemClick { _, data, _ -> onItemClick(data) }
    }

    private val renderer = ManagedRenderer(binding, adapter)

    init {
        FilterChipRow.setOnSingleSelectionChangeListener(
            binding.managedFilterChipRow.root,
            R.id.managed_chipGroup,
        ) { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.managed_chipEnabled -> ManagedFilter.ENABLED
                R.id.managed_chipPending -> ManagedFilter.PENDING
                else -> ManagedFilter.ALL
            }
            onManagedFilterChanged(filter)
        }
    }

    fun render(state: ConfigUiState): Int = renderer.render(state)
    fun setVisibility(visible: Boolean) = renderer.setVisibility(visible)
}
