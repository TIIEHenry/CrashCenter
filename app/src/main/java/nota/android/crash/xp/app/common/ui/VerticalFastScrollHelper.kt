package nota.android.crash.xp.app.common.ui

import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import nota.android.crash.xp.app.R

/**
 * Fixed-height vertical fast scroll for long lists.
 *
 * RecyclerView's built-in [androidx.recyclerview.widget.FastScroller] shrinks the thumb as
 * content grows (`extent² / range`). This helper keeps a constant thumb height and maps drag
 * position linearly along the track.
 */
class VerticalFastScrollHelper private constructor(
    private val recyclerView: RecyclerView,
) : RecyclerView.ItemDecoration(), RecyclerView.OnItemTouchListener {

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            if (state == STATE_DRAGGING) return
            syncThumbFromScrollOffset()
            setState(STATE_VISIBLE)
            scheduleHide()
            invalidateThumb(rv)
        }
    }

    private val resources = recyclerView.resources
    private val thumbHeightPx = resources.getDimensionPixelSize(R.dimen.fastscroll_thumb_height)
    private val trackWidthPx = resources.getDimensionPixelSize(R.dimen.fastscroll_track_width)
    private val marginEndPx = resources.getDimensionPixelSize(R.dimen.fastscroll_margin_end)
    private val touchWidthPx = max(trackWidthPx, resources.getDimensionPixelSize(R.dimen.list_item_min_height))
    private val hideDelayMs = resources.getInteger(R.integer.fastscroll_hide_delay_ms).toLong()

    private val thumbDrawable = AppCompatResources.getDrawable(
        recyclerView.context,
        R.drawable.fastscroll_thumb_vertical,
    )!!.mutate()
    private val trackDrawable = AppCompatResources.getDrawable(
        recyclerView.context,
        R.drawable.fastscroll_track_vertical,
    )!!.mutate()

    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { setState(STATE_HIDDEN) }

    private var state = STATE_HIDDEN
    private var thumbOffsetY = 0f
    private var dragGrabOffsetY = 0f
    private var lastDragPosition = RecyclerView.NO_POSITION
    private var savedItemAnimator: RecyclerView.ItemAnimator? = null

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (this.state == STATE_HIDDEN || !isScrollable()) return

        val trackLeft = parent.width - parent.paddingRight - marginEndPx - trackWidthPx
        val trackTop = parent.paddingTop
        val trackBottom = parent.height - parent.paddingBottom

        trackDrawable.setBounds(trackLeft, trackTop, trackLeft + trackWidthPx, trackBottom)
        trackDrawable.draw(canvas)

        val pressed = this.state == STATE_DRAGGING
        thumbDrawable.state = if (pressed) PRESSED_STATE else EMPTY_STATE
        val thumbTop = trackTop + thumbOffsetY.toInt()
        thumbDrawable.setBounds(trackLeft, thumbTop, trackLeft + trackWidthPx, thumbTop + thumbHeightPx)
        thumbDrawable.draw(canvas)
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN || state == STATE_HIDDEN) return false
        if (!isNearThumb(event.x, event.y)) return false
        rv.parent?.requestDisallowInterceptTouchEvent(true)
        rv.stopScroll()
        savedItemAnimator = rv.itemAnimator
        rv.itemAnimator = null
        setState(STATE_DRAGGING)
        dragGrabOffsetY = event.y - (rv.paddingTop + thumbOffsetY)
        lastDragPosition = RecyclerView.NO_POSITION
        handler.removeCallbacks(hideRunnable)
        return true
    }

    override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val travel = thumbTravel()
                thumbOffsetY = (event.y - rv.paddingTop - dragGrabOffsetY)
                    .coerceIn(0f, travel.toFloat())
                scrollToThumbOffset(thumbOffsetY)
                invalidateThumb(rv)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                rv.itemAnimator = savedItemAnimator
                savedItemAnimator = null
                lastDragPosition = RecyclerView.NO_POSITION
                syncThumbFromScrollOffset()
                setState(STATE_VISIBLE)
                scheduleHide()
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit

    private fun isScrollable(): Boolean {
        val range = recyclerView.computeVerticalScrollRange()
        val extent = recyclerView.computeVerticalScrollExtent()
        return range > extent
    }

    private fun scrollMetrics(): ScrollMetrics? {
        val range = recyclerView.computeVerticalScrollRange()
        val extent = recyclerView.computeVerticalScrollExtent()
        if (range <= extent) return null
        return ScrollMetrics(
            range = range,
            extent = extent,
            offset = recyclerView.computeVerticalScrollOffset(),
        )
    }

    private fun thumbTravel(): Int {
        val trackHeight = recyclerView.height - recyclerView.paddingTop - recyclerView.paddingBottom
        return (trackHeight - thumbHeightPx).coerceAtLeast(0)
    }

    private fun syncThumbFromScrollOffset() {
        val metrics = scrollMetrics() ?: return
        val travel = thumbTravel()
        if (travel <= 0) return
        val maxOffset = metrics.range - metrics.extent
        thumbOffsetY = metrics.offset.toFloat() / maxOffset * travel
    }

    private fun scrollToThumbOffset(offsetY: Float) {
        val travel = thumbTravel()
        if (travel <= 0) return
        val progress = (offsetY / travel).coerceIn(0f, 1f)
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        if (itemCount <= 1) return

        val targetPosition = (progress * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
        if (targetPosition == lastDragPosition) return
        lastDragPosition = targetPosition
        layoutManager.scrollToPositionWithOffset(targetPosition, 0)
    }

    private fun invalidateThumb(rv: RecyclerView) {
        rv.invalidate()
    }

    private fun isNearThumb(x: Float, y: Float): Boolean {
        val touchLeft = recyclerView.width - recyclerView.paddingRight - marginEndPx - touchWidthPx
        val thumbTop = recyclerView.paddingTop + thumbOffsetY
        val thumbBottom = thumbTop + thumbHeightPx
        return x >= touchLeft && y >= thumbTop && y <= thumbBottom
    }

    private fun setState(newState: Int) {
        if (state == newState) return
        state = newState
        invalidateThumb(recyclerView)
    }

    private fun scheduleHide() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, hideDelayMs)
    }

    private data class ScrollMetrics(
        val range: Int,
        val extent: Int,
        val offset: Int,
    )

    companion object {
        private const val STATE_HIDDEN = 0
        private const val STATE_VISIBLE = 1
        private const val STATE_DRAGGING = 2
        private val PRESSED_STATE = intArrayOf(android.R.attr.state_pressed)
        private val EMPTY_STATE = intArrayOf()

        fun attach(recyclerView: RecyclerView) {
            if (recyclerView.getTag(R.id.recycler_vertical_fast_scroll) != null) return
            val helper = VerticalFastScrollHelper(recyclerView)
            recyclerView.addItemDecoration(helper)
            recyclerView.addOnItemTouchListener(helper)
            recyclerView.addOnScrollListener(helper.scrollListener)
            recyclerView.setTag(R.id.recycler_vertical_fast_scroll, helper)
        }
    }
}
