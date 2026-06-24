package nota.android.crash.xp.app.common.ui

import android.content.res.ColorStateList
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import nota.android.crash.xp.app.R
import kotlin.math.abs

private const val SHEET_HEIGHT_HALF_RATIO = 0.5f
private const val SHEET_DETAIL_HALF_EXPANDED_RATIO = 0.85f

/**
 * Applies the standard CrashCenter bottom-sheet appearance:
 * rounded top corners, surface-colored background with outline stroke,
 * half-screen peek height, and fit-to-parent height when expanded.
 */
fun BottomSheetDialogFragment.configureBottomSheetAppearance() {
    val sheetDialog = dialog as? BottomSheetDialog ?: return
    val bottomSheet = sheetDialog.findViewById<View>(
        com.google.android.material.R.id.design_bottom_sheet,
    ) ?: return

    val radius = resources.getDimension(R.dimen.radius_mobile_sheet)
    val shapeAppearance = ShapeAppearanceModel.builder()
        .setTopLeftCorner(CornerFamily.ROUNDED, radius)
        .setTopRightCorner(CornerFamily.ROUNDED, radius)
        .build()
    val sheetBackground = MaterialShapeDrawable(shapeAppearance).apply {
        fillColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.surface),
        )
        setStroke(
            1.0f,
            ContextCompat.getColor(requireContext(), R.color.outlineVariant),
        )
    }
    bottomSheet.background = sheetBackground
    // Prevent M3 from applying tonal surface tint on top of the solid Fluent layer fill
    sheetBackground.setTintList(null)
    bottomSheet.clipToOutline = true
    bottomSheet.elevation = resources.getDimension(R.dimen.sheet_elevation)

    val behavior = BottomSheetBehavior.from(bottomSheet)
    val displayHeight = resources.displayMetrics.heightPixels
    behavior.peekHeight = (displayHeight * SHEET_HEIGHT_HALF_RATIO).toInt()
    behavior.isFitToContents = false
    behavior.skipCollapsed = false
    behavior.state = BottomSheetBehavior.STATE_COLLAPSED

    bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
        height = ViewGroup.LayoutParams.MATCH_PARENT
    }
}

/**
 * Crash detail sheet: uses a real 85% height at rest and a real 100% height
 * when expanded. Only [dragHandle] resizes the sheet; the CodeEditor content
 * itself never drags the container.
 */
fun BottomSheetDialogFragment.configureCrashDetailBottomSheetAppearance(
    sheetContentRoot: View,
    dragHandle: View,
) {
    val sheetDialog = dialog as? BottomSheetDialog ?: return
    val bottomSheet = sheetDialog.findViewById<View>(
        com.google.android.material.R.id.design_bottom_sheet,
    ) ?: return

    val radius = resources.getDimension(R.dimen.radius_mobile_sheet)
    val shapeAppearance = ShapeAppearanceModel.builder()
        .setTopLeftCorner(CornerFamily.ROUNDED, radius)
        .setTopRightCorner(CornerFamily.ROUNDED, radius)
        .build()
    val sheetBackground = MaterialShapeDrawable(shapeAppearance).apply {
        fillColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.surface),
        )
        setStroke(
            1.0f,
            ContextCompat.getColor(requireContext(), R.color.outlineVariant),
        )
    }
    bottomSheet.background = sheetBackground
    sheetBackground.setTintList(null)
    bottomSheet.clipToOutline = true
    bottomSheet.elevation = resources.getDimension(R.dimen.sheet_elevation)

    val displayHeight = resources.displayMetrics.heightPixels
    val halfHeight = (displayHeight * SHEET_DETAIL_HALF_EXPANDED_RATIO).toInt()
    val fullHeight = displayHeight

    val behavior = BottomSheetBehavior.from(bottomSheet)
    behavior.isFitToContents = true
    behavior.isDraggable = false
    behavior.skipCollapsed = true
    behavior.isHideable = false

    bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
        height = halfHeight
    }
    behavior.state = BottomSheetBehavior.STATE_EXPANDED

    fun applySheetHeight(targetHeight: Int) {
        val clamped = targetHeight.coerceIn(halfHeight, fullHeight)
        if (bottomSheet.layoutParams.height != clamped) {
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = clamped
            }
            bottomSheet.requestLayout()
        }
    }

    applySheetHeight(halfHeight)

    val touchSlop = android.view.ViewConfiguration.get(requireContext()).scaledTouchSlop
    var startRawY = 0f
    var startHeight = halfHeight
    var dragging = false

    dragHandle.setOnTouchListener { _, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startRawY = event.rawY
                startHeight = bottomSheet.layoutParams.height
                dragging = false
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = event.rawY - startRawY
                if (!dragging && abs(dy) > touchSlop) {
                    dragging = true
                }
                if (dragging) {
                    applySheetHeight((startHeight - dy).toInt())
                }
                true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    val currentHeight = bottomSheet.layoutParams.height
                    val midpoint = halfHeight + ((fullHeight - halfHeight) / 2)
                    val snapHeight = if (currentHeight >= midpoint) fullHeight else halfHeight
                    applySheetHeight(snapHeight)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    dragging = false
                    true
                } else {
                    false
                }
            }

            else -> false
        }
    }

    behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(sheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) dismissAllowingStateLoss()
        }

        override fun onSlide(sheet: View, slideOffset: Float) = Unit
    })

    sheetContentRoot.layoutParams = sheetContentRoot.layoutParams.apply {
        height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    sheetDialog.onBackPressedDispatcher.addCallback(
        this.viewLifecycleOwner,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentHeight = bottomSheet.layoutParams.height
                if (currentHeight >= fullHeight - 10) {
                    applySheetHeight(halfHeight)
                } else {
                    dismiss()
                }
            }
        },
    )
}
