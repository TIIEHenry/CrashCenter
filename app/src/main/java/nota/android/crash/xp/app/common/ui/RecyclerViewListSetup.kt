package nota.android.crash.xp.app.common.ui

import android.content.Context
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.R

/**
 * Shared RecyclerView list setup: vertical layout, optional spacing, fixed-height fast scroll.
 * Thumb/track drawables come from theme resources; behavior is [VerticalFastScrollHelper].
 */
object RecyclerViewListSetup {

    fun apply(
        recyclerView: RecyclerView,
        context: Context,
        @DimenRes spacingDimen: Int = R.dimen.spacing_xs,
        addSpacingDecoration: Boolean = true,
    ) {
        recyclerView.apply {
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            if (layoutManager == null) {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            }
            if (addSpacingDecoration && !hasVerticalSpacingDecoration()) {
                addItemDecoration(
                    VerticalSpacingItemDecoration(
                        context.resources.getDimensionPixelSize(spacingDimen),
                    ),
                )
            }
            VerticalFastScrollHelper.attach(this)
        }
    }

    private fun RecyclerView.hasVerticalSpacingDecoration(): Boolean {
        for (index in 0 until itemDecorationCount) {
            if (getItemDecorationAt(index) is VerticalSpacingItemDecoration) return true
        }
        return false
    }
}
