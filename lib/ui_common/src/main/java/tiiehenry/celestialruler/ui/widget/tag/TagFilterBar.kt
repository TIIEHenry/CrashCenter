package tiiehenry.celestialruler.ui.widget.tag

import android.view.ContextThemeWrapper
import android.view.View
import androidx.annotation.StyleRes
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import tiiehenry.celestialruler.ui.R

class TagFilterBar(
    private val chipGroup: ChipGroup,
    @StyleRes private val chipStyle: Int = R.style.Widget_CelestialRuler_TagChip_Filter,
) {
    private val chipContext = ContextThemeWrapper(
        chipGroup.context,
        R.style.ThemeOverlay_CelestialRuler_MaterialChip,
    )
    private val chipIdToTag = mutableMapOf<Int, String>()
    private var selectionListener: ((Set<String>) -> Unit)? = null
    private var suppressEvents = false

    var onChipViewCreated: ((View) -> Unit)? = null

    init {
        chipGroup.isSingleSelection = false
        chipGroup.isSelectionRequired = false
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressEvents) return@setOnCheckedStateChangeListener
            val selected = checkedIds.mapNotNull { chipIdToTag[it] }.toSet()
            selectionListener?.invoke(selected)
        }
    }

    fun setOnSelectionChanged(listener: (Set<String>) -> Unit) {
        selectionListener = listener
    }

    fun getSelectedTags(): Set<String> {
        return chipGroup.checkedChipIds.mapNotNull { chipIdToTag[it] }.toSet()
    }

    fun setTags(tags: List<String>, selected: Set<String> = emptySet()) {
        suppressEvents = true
        chipGroup.removeAllViews()
        chipIdToTag.clear()
        for (tag in tags) {
            val chip = Chip(chipContext, null, chipStyle).apply {
                text = tag
                isCheckable = true
                isChecked = selected.contains(tag)
                id = View.generateViewId()
            }
            chipIdToTag[chip.id] = tag
            onChipViewCreated?.invoke(chip)
            chipGroup.addView(chip)
        }
        suppressEvents = false
    }

    fun clearTags() {
        setTags(emptyList())
    }
}
