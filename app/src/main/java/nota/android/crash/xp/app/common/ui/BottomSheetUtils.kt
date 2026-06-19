package nota.android.crash.xp.app.common.ui

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import nota.android.crash.xp.app.R

private const val SHEET_HEIGHT_HALF_RATIO = 0.5f

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
