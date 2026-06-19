package tiiehenry.celestialruler.ui.interaction

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import tiiehenry.celestialruler.ui.R

/**
 * Clarence popup strategy: TouchPrimary long-press → [PopupPlacement.PressCentered],
 * PointerPrimary right-click → [PopupPlacement.AtPointer].
 *
 * All row / tree / preference context-menu entry points should wire through here so
 * long-press ≡ right-click equivalence holds.
 */
object PopupStrategy {

    enum class Trigger {
        LongPress,
        ContextClick,
    }

    @JvmStatic
    fun placement(modality: InputModality, trigger: Trigger): PopupPlacement {
        return when {
            trigger == Trigger.ContextClick -> PopupPlacement.AtPointer
            modality == InputModality.PointerPrimary -> PopupPlacement.AtPointer
            else -> PopupPlacement.PressCentered
        }
    }

    @JvmStatic
    fun popupCornerRadiusPx(context: Context, modality: InputModality): Int {
        val dimen = when (modality) {
            InputModality.PointerPrimary -> R.dimen.cr_radius_pc_overlay
            InputModality.TouchPrimary -> R.dimen.cr_radius_mobile_popup
        }
        return context.resources.getDimensionPixelSize(dimen)
    }

    /**
     * Wire long-press and right-click (API 23+) to the same handler.
     * [onTrigger] receives screen coordinates of the press / pointer.
     */
    @JvmStatic
    fun wireContextMenuTrigger(
        view: View,
        onTrigger: (trigger: Trigger, pointerX: Float, pointerY: Float) -> Boolean,
    ) {
        view.setOnLongClickListener { v ->
            val (x, y) = viewCenterOnScreen(v)
            onTrigger(Trigger.LongPress, x, y)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.setOnContextClickListener { v ->
                val (x, y) = viewCenterOnScreen(v)
                onTrigger(Trigger.ContextClick, x, y)
            }
        }
    }

    /**
     * Show a simple item list using PressCentered dialog or AtPointer popup per strategy.
     */
    @JvmStatic
    fun showItemList(
        context: Context,
        anchor: View,
        modality: InputModality,
        trigger: Trigger,
        pointerX: Float,
        pointerY: Float,
        title: CharSequence?,
        items: Array<CharSequence>,
        onItemClick: (Int) -> Unit,
    ) {
        when (placement(modality, trigger)) {
            PopupPlacement.PressCentered -> {
                AlertDialog.Builder(context)
                    .apply { if (title != null) setTitle(title) }
                    .setItems(items) { _, which -> onItemClick(which) }
                    .show()
            }
            PopupPlacement.AtPointer -> {
                showAtPointerList(context, anchor, pointerX, pointerY, modality, items, onItemClick)
            }
        }
    }

    /**
     * Run [action] when context menu is triggered (long-press or right-click).
     */
    @JvmStatic
    fun wireContextAction(
        view: View,
        action: () -> Unit,
    ) {
        wireContextMenuTrigger(view) { _, _, _ ->
            action()
            true
        }
    }

    private fun showAtPointerList(
        context: Context,
        anchor: View,
        pointerX: Float,
        pointerY: Float,
        modality: InputModality,
        items: Array<CharSequence>,
        onItemClick: (Int) -> Unit,
    ) {
        val radius = popupCornerRadiusPx(context, modality).toFloat()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, R.color.cr_layer))
                cornerRadius = radius
                setStroke(
                    context.resources.getDimensionPixelSize(R.dimen.cr_elevation_flat),
                    ContextCompat.getColor(context, R.color.cr_stroke),
                )
            }
            background = bg
            clipToOutline = true
            elevation = context.resources.getDimension(R.dimen.cr_elevation_max)
        }
        val rowPaddingV = context.resources.getDimensionPixelSize(
            R.dimen.cr_spacing_menu_row_padding_vertical,
        )
        val rowPaddingH = context.resources.getDimensionPixelSize(
            R.dimen.cr_spacing_content_padding_horizontal,
        )
        items.forEachIndexed { index, label ->
            val row = TextView(context).apply {
                text = label
                setTextColor(ContextCompat.getColor(context, R.color.cr_text_primary))
                textSize = 14f
                setPadding(rowPaddingH, rowPaddingV, rowPaddingH, rowPaddingV)
                background = ContextCompat.getDrawable(context, R.drawable.cr_ripple_row_overlay)
                setOnClickListener {
                    onItemClick(index)
                }
            }
            container.addView(row, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
        }
        val popup = PopupWindow(
            container,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            elevation = context.resources.getDimension(R.dimen.cr_elevation_max)
        }
        container.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val pw = container.measuredWidth
        val ph = container.measuredHeight
        val display = context.resources.displayMetrics
        var x = pointerX.toInt()
        var y = pointerY.toInt()
        if (x + pw > display.widthPixels) {
            x = (display.widthPixels - pw).coerceAtLeast(0)
        }
        if (y + ph > display.heightPixels) {
            y = (display.heightPixels - ph).coerceAtLeast(0)
        }
        popup.showAtLocation(
            anchor,
            Gravity.NO_GRAVITY,
            x,
            y,
        )
    }

    private fun viewCenterOnScreen(view: View): Pair<Float, Float> {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return loc[0] + view.width / 2f to loc[1] + view.height / 2f
    }
}
