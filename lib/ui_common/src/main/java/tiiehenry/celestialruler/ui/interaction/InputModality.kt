package tiiehenry.celestialruler.ui.interaction

/**
 * Clarence interaction-language input form. Not equivalent to OS platform label.
 */
enum class InputModality {
    /** Touch-first: long-press triggers PressCentered popups. */
    TouchPrimary,

    /** Pointer-first: right-click triggers AtPointer popups; hover available. */
    PointerPrimary,
}
