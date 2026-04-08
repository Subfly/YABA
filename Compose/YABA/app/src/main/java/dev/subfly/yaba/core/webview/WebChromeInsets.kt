package dev.subfly.yaba.core.webview

/**
 * Total top inset (px) for WebView content as `--yaba-web-chrome-status-bar` (see [effectiveWebViewTopChromeInsetPx]).
 */
data class WebChromeInsets(
    val topChromeInsetPx: Int,
)

/**
 * Pixels from the top of the WebView document to reserve so content clears the overlaid top app bar.
 *
 * - [overlayTopAppBarPx] — Material top app bar row height (below status bar), e.g. 64.dp in px.
 *
 * Formula: `max(0, overlayTopAppBarPx)`.
 *
 * On our Compose layout, the WebView is already positioned relative to the system bar region, so
 * adding status-bar inset again over-shifts the editor on some devices (especially foldables).
 */
fun effectiveWebViewTopChromeInsetPx(
    overlayTopAppBarPx: Int,
): Int = overlayTopAppBarPx.coerceAtLeast(0)
