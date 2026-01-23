package dev.subfly.yaba.util

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.window.core.layout.WindowSizeClass

/**
 * Information about the current window's pane layout configuration.
 *
 * @property isTwoPaneLayout True if both list and detail panes are shown simultaneously.
 * @property isDetailPaneLargerThanList True if the detail pane has more space than the list pane.
 * This is useful for determining whether to show more UI elements directly in the detail pane
 * (e.g., more items in the top app bar) or to fit them into a "more options" button.
 */
data class PaneLayoutInfo(
    val isTwoPaneLayout: Boolean,
    val isDetailPaneLargerThanList: Boolean
)

/**
 * Gets information about the current window's pane layout configuration.
 *
 * This composable determines:
 * - Whether the app is showing a two-pane layout (list + detail simultaneously)
 * - Whether the detail pane is larger than the list pane
 *
 * Based on Material Design 3 adaptive layout guidelines:
 * - Two-pane layout typically appears when width >= 600dp (Medium breakpoint)
 * - Detail pane is typically larger than list pane when width >= 840dp (Expanded breakpoint)
 * - List pane is typically fixed at around 360dp
 *
 * @param supportLargeAndXLargeWidth Whether to support large and extra-large width breakpoints.
 * Defaults to true for better desktop support.
 *
 * @return [PaneLayoutInfo] containing information about the current pane layout.
 *
 * @sample
 * ```
 * val paneInfo = rememberPaneLayoutInfo()
 * if (paneInfo.isDetailPaneLargerThanList) {
 *     // Show more items directly in the top app bar
 *     TopAppBarWithMoreItems()
 * } else {
 *     // Fit items into a "more options" button
 *     TopAppBarWithMoreOptionsButton()
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberPaneLayoutInfo(
    supportLargeAndXLargeWidth: Boolean = true
): PaneLayoutInfo {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo(
        supportLargeAndXLargeWidth = supportLargeAndXLargeWidth
    )

    return remember(windowAdaptiveInfo) {
        val windowSizeClass = windowAdaptiveInfo.windowSizeClass
        
        // Two-pane layout is typically shown when width is at least Medium (600dp)
        val isTwoPaneLayout = windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
        )
        
        // Detail pane is typically larger than list pane when width is at least Expanded (840dp)
        // The list pane is typically fixed at around 360dp, so when total width >= 840dp,
        // the detail pane (remaining space) would be >= 480dp, which is larger than 360dp
        // Using 840 as the breakpoint (Expanded width lower bound per Material Design guidelines)
        val isDetailPaneLargerThanList = windowSizeClass.isWidthAtLeastBreakpoint(840)
        
        PaneLayoutInfo(
            isTwoPaneLayout = isTwoPaneLayout,
            isDetailPaneLargerThanList = isDetailPaneLargerThanList
        )
    }
}
