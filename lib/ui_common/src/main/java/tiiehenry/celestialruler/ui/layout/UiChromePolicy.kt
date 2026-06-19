package tiiehenry.celestialruler.ui.layout

import android.content.res.Configuration

/**
 * Derives floating chrome anchors and list content insets from [UiWindowTier] and orientation.
 *
 * InputModality stays orthogonal (ADR-006); this policy only handles spatial layout.
 */
object UiChromePolicy {

    enum class FloatBarAnchor {
        /** Bottom-end, typically stacked above the tab bar (portrait phone). */
        BOTTOM_END,
        /** End edge, vertically centered (landscape / medium+ to avoid bottom stacking). */
        END_CENTER,
    }

    enum class TabBarAnchor {
        BOTTOM_CENTER,
    }

    data class ChromeMetrics(
        val floatBarWidthPx: Int,
        val floatBarMarginEndPx: Int,
        val floatBarMarginBottomPx: Int,
        val tabBarHeightPx: Int,
        val tabBarWidthPx: Int,
        val tabBarBottomMarginPx: Int,
        val tabContentGapPx: Int,
        val listBottomPaddingPx: Int,
    )

    data class ContentInsets(
        val paddingEndPx: Int,
        val paddingBottomPx: Int,
    )

    data class HubChromeLayout(
        val floatBarAnchor: FloatBarAnchor,
        val tabBarAnchor: TabBarAnchor,
        val contentInsets: ContentInsets,
    )

    @JvmStatic
    fun manageContentInsets(tier: UiWindowTier, metrics: ChromeMetrics): ContentInsets {
        val end = metrics.floatBarWidthPx + metrics.floatBarMarginEndPx + metrics.tabContentGapPx
        return ContentInsets(
            paddingEndPx = end,
            paddingBottomPx = metrics.listBottomPaddingPx,
        )
    }

    @JvmStatic
    fun hubChromeLayout(configuration: Configuration, metrics: ChromeMetrics): HubChromeLayout {
        val tier = UiWindowTier.from(configuration)
        val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val splitBottomChrome = landscape && tier != UiWindowTier.Compact

        val endInset = if (splitBottomChrome) {
            metrics.floatBarWidthPx + metrics.floatBarMarginEndPx + metrics.tabContentGapPx
        } else {
            maxOf(
                metrics.floatBarWidthPx + metrics.floatBarMarginEndPx,
                metrics.tabBarWidthPx / 2,
            ) + metrics.tabContentGapPx
        }

        return if (splitBottomChrome) {
            HubChromeLayout(
                floatBarAnchor = FloatBarAnchor.END_CENTER,
                tabBarAnchor = TabBarAnchor.BOTTOM_CENTER,
                contentInsets = ContentInsets(
                    paddingEndPx = endInset,
                    paddingBottomPx = metrics.listBottomPaddingPx,
                ),
            )
        } else {
            HubChromeLayout(
                floatBarAnchor = FloatBarAnchor.BOTTOM_END,
                tabBarAnchor = TabBarAnchor.BOTTOM_CENTER,
                contentInsets = ContentInsets(
                    paddingEndPx = endInset,
                    paddingBottomPx = metrics.listBottomPaddingPx,
                ),
            )
        }
    }
}
