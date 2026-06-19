package tiiehenry.celestialruler.ui.interaction

/**
 * Clarence popup placement tier driven by [InputModality] and trigger kind.
 */
enum class PopupPlacement {
    /** Long-press / touch context: centered on press region or screen. */
    PressCentered,

    /** Pointer context click: anchored at cursor / pointer coordinates. */
    AtPointer,
}
