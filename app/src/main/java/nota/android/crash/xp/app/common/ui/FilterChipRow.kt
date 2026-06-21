package nota.android.crash.xp.app.common.ui

import android.view.View
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

object FilterChipRow {

    fun chipGroup(root: View, chipGroupId: Int): ChipGroup = root.findViewById(chipGroupId)

    fun chip(root: View, chipGroupId: Int, chipId: Int): Chip? = chipGroup(root, chipGroupId).findViewById(chipId)

    fun setCountLabel(root: View, countLabelId: Int, text: CharSequence) {
        root.findViewById<TextView>(countLabelId)?.text = text
    }

    fun setOnSingleSelectionChangeListener(
        root: View,
        chipGroupId: Int,
        listener: ChipGroup.OnCheckedStateChangeListener,
    ) {
        chipGroup(root, chipGroupId).setOnCheckedStateChangeListener(listener)
    }

    fun setChipChecked(root: View, chipGroupId: Int, chipId: Int, checked: Boolean) {
        chip(root, chipGroupId, chipId)?.isChecked = checked
    }
}
