package dev.subfly.yaba.core.components.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.subfly.yabacore.webview.WebViewEditorBridge
import dev.subfly.yabacore.webview.WebViewReaderBridge
import dev.subfly.yabacore.webview.MathTapEvent
import dev.subfly.yabacore.webview.YabaWebFeature
import dev.subfly.yabacore.webview.YabaWebHostEvent
import dev.subfly.yabacore.webview.YabaWebScrollDirection

/**
 * Single entry WebView for readable HTML viewer, rich-text editor, HTML/PDF converter shells, and PDF viewer.
 *
 * @param onHostEvent Load state, reader metrics, and converter results
 * @param onReaderBridgeReady Non-null when the reader bridge is available ([YabaWebFeature.ReadableViewer], [YabaWebFeature.PdfViewer])
 */
@Composable
fun YabaWebView(
    modifier: Modifier = Modifier,
    baseUrl: String,
    feature: YabaWebFeature,
    onHostEvent: (YabaWebHostEvent) -> Unit = {},
    onUrlClick: (String) -> Boolean = { false },
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit = {},
    onReaderBridgeReady: (WebViewReaderBridge?) -> Unit = {},
    onEditorBridgeReady: (WebViewEditorBridge?) -> Unit = {},
    onHighlightTap: (String) -> Unit = {},
    onMathTap: (MathTapEvent) -> Unit = {},
) {
    YabaWebViewHost(
        modifier = modifier,
        baseUrl = baseUrl,
        feature = feature,
        onHostEvent = onHostEvent,
        onUrlClick = onUrlClick,
        onScrollDirectionChanged = onScrollDirectionChanged,
        onReaderBridgeReady = onReaderBridgeReady,
        onEditorBridgeReady = onEditorBridgeReady,
        onHighlightTap = onHighlightTap,
        onMathTap = onMathTap,
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
    onReaderBridgeReady: (WebViewReaderBridge?) -> Unit,
    onEditorBridgeReady: (WebViewEditorBridge?) -> Unit,
    onHighlightTap: (String) -> Unit,
    onMathTap: (MathTapEvent) -> Unit,
)
