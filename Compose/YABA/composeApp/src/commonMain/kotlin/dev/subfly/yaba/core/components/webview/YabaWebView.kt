package dev.subfly.yaba.core.components.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.subfly.yabacore.webview.WebViewReaderBridge
import dev.subfly.yabacore.webview.YabaWebFeature
import dev.subfly.yabacore.webview.YabaWebHostEvent
import dev.subfly.yabacore.webview.YabaWebScrollDirection

/**
 * Single entry WebView for markdown viewer, editor, HTML/PDF converter shells, and PDF viewer.
 *
 * @param onHostEvent Load state, reader metrics, and converter results
 * @param onBridgeReady Non-null when the reader bridge is available ([YabaWebFeature.MarkdownViewer], [YabaWebFeature.PdfViewer])
 */
@Composable
fun YabaWebView(
    modifier: Modifier = Modifier,
    baseUrl: String,
    feature: YabaWebFeature,
    onHostEvent: (YabaWebHostEvent) -> Unit = {},
    onUrlClick: (String) -> Boolean = { false },
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit = {},
    onBridgeReady: (WebViewReaderBridge?) -> Unit = {},
    onHighlightTap: (String) -> Unit = {},
) {
    YabaWebViewHost(
        modifier = modifier,
        baseUrl = baseUrl,
        feature = feature,
        onHostEvent = onHostEvent,
        onUrlClick = onUrlClick,
        onScrollDirectionChanged = onScrollDirectionChanged,
        onBridgeReady = onBridgeReady,
        onHighlightTap = onHighlightTap,
    )
}

@Composable
internal expect fun YabaWebViewHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onUrlClick: (String) -> Boolean,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onBridgeReady: (WebViewReaderBridge?) -> Unit,
    onHighlightTap: (String) -> Unit,
)
