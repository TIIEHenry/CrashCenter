package nota.android.crash.xp.app.config

import android.view.View
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.common.ui.adapter.BaseListAdapter
import nota.android.crash.xp.app.databinding.FragmentConfigBinding

class FilterConfig<T : Any>(
    val chipRowRoot: View,
    val chipGroupId: Int,
    val chipToFilter: Map<Int, T>,
    val defaultFilter: T,
    val onFilterChanged: (T) -> Unit,
)

class ConfigListController<T : Any>(
    binding: FragmentConfigBinding,
    adapter: BaseListAdapter<T, *>,
    countLabelId: Int,
    dataSelector: (ConfigUiState) -> List<T>,
    filterConfig: FilterConfig<*>,
) {
    private val renderer = AppListRenderer(
        recyclerView = binding.recyclerv,
        filterChipRowRoot = filterConfig.chipRowRoot,
        countLabelId = countLabelId,
        adapter = adapter,
        dataSelector = dataSelector,
    )

    init {
        @Suppress("UNCHECKED_CAST")
        val config = filterConfig as FilterConfig<Any>
        FilterChipRow.setOnSingleSelectionChangeListener(
            config.chipRowRoot,
            config.chipGroupId,
        ) { _, checkedIds ->
            val filter = config.chipToFilter[checkedIds.firstOrNull()]
                ?: config.defaultFilter
            config.onFilterChanged(filter)
        }
    }

    fun render(state: ConfigUiState): Int = renderer.render(state)
    fun setVisibility(visible: Boolean) = renderer.setVisibility(visible)
}
