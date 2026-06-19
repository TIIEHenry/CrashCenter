package nota.android.crash.xp.app.common.ui

import android.view.View
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import nota.android.crash.xp.app.R

object FilterChipRow {

    fun chipGroup(root: View): ChipGroup = root.findViewById(R.id.chipGroup)

    fun chip(root: View, chipId: Int): Chip? = chipGroup(root).findViewById(chipId)

    fun setCountLabel(root: View, text: CharSequence) {
        root.findViewById<TextView>(R.id.countLabel)?.text = text
    }

    fun setOnSingleSelectionChangeListener(
        root: View,
        listener: ChipGroup.OnCheckedStateChangeListener,
    ) {
        chipGroup(root).setOnCheckedStateChangeListener(listener)
    }

    fun setChipChecked(root: View, chipId: Int, checked: Boolean) {
        chip(root, chipId)?.isChecked = checked
    }
}
