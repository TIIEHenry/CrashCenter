package nota.android.crash.xp.app.config

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.common.ui.adapter.BaseListAdapter

class AppListRenderer<T : Any>(
    private val recyclerView: RecyclerView,
    private val filterChipRowRoot: View,
    private val countLabelId: Int,
    private val adapter: BaseListAdapter<T, *>,
    private val dataSelector: (ConfigUiState) -> List<T>,
) {
    fun render(state: ConfigUiState): Int {
        recyclerView.adapter = adapter
        val list = dataSelector(state)
        adapter.submitList(list)
        val count = list.size
        FilterChipRow.setCountLabel(
            filterChipRowRoot,
            countLabelId,
            filterChipRowRoot.context.resources.getQuantityString(R.plurals.app_count, count, count),
        )
        return count
    }

    fun setVisibility(visible: Boolean) {
        filterChipRowRoot.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
