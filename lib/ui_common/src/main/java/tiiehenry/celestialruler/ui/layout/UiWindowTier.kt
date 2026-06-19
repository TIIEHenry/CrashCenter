package tiiehenry.celestialruler.ui.layout

import android.content.res.Configuration

/**
 * Runtime window width tier for adaptive chrome and navigation policy (ADR-006).
 *
 * Breakpoints align with Material window size classes using [Configuration.screenWidthDp].
 */
enum class UiWindowTier {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        /** Compact &lt; 600dp, Medium 600–839dp, Expanded ≥ 840dp. */
        @JvmStatic
        fun from(configuration: Configuration): UiWindowTier =
            fromWidthDp(configuration.screenWidthDp)

        @JvmStatic
        fun fromWidthDp(widthDp: Int): UiWindowTier = when {
            widthDp >= 840 -> Expanded
            widthDp >= 600 -> Medium
            else -> Compact
        }
    }
}
