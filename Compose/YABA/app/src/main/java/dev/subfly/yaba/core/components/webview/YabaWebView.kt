package dev.subfly.yaba.core.components.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.subfly.yaba.core.webview.InlineLinkTapEvent
import dev.subfly.yaba.core.webview.InlineMentionTapEvent
import dev.subfly.yaba.core.webview.MathTapEvent
import dev.subfly.yaba.core.webview.WebViewEditorBridge
import dev.subfly.yaba.core.webview.WebViewReaderBridge
import dev.subfly.yaba.core.webview.WebViewCanvasBridge
import dev.subfly.yaba.core.webview.YabaWebFeature
import dev.subfly.yaba.core.webview.YabaWebHostEvent
import dev.subfly.yaba.core.webview.YabaWebScrollDirection

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
    onCanvasBridgeReady: (WebViewCanvasBridge?) -> Unit = {},
    onAnnotationTap: (String) -> Unit = {},
    onMathTap: (MathTapEvent) -> Unit = {},
    onInlineLinkTap: (InlineLinkTapEvent) -> Unit = {},
    onInlineMentionTap: (InlineMentionTapEvent) -> Unit = {},
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
        onCanvasBridgeReady = onCanvasBridgeReady,
        onAnnotationTap = onAnnotationTap,
        onMathTap = onMathTap,
        onInlineLinkTap = onInlineLinkTap,
        onInlineMentionTap = onInlineMentionTap,
    )
}

@Composable
internal fun YabaWebViewHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onUrlClick: (String) -> Boolean,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReaderBridgeReady: (WebViewReaderBridge?) -> Unit,
    onEditorBridgeReady: (WebViewEditorBridge?) -> Unit,
    onCanvasBridgeReady: (WebViewCanvasBridge?) -> Unit,
    onAnnotationTap: (String) -> Unit,
    onMathTap: (MathTapEvent) -> Unit,
    onInlineLinkTap: (InlineLinkTapEvent) -> Unit,
    onInlineMentionTap: (InlineMentionTapEvent) -> Unit,
) {
    when (val f = feature) {
        is YabaWebFeature.ReadableViewer ->
            YabaReadableViewerFeatureHost(
                modifier = modifier,
                baseUrl = baseUrl,
                feature = f,
                onHostEvent = onHostEvent,
                onUrlClick = onUrlClick,
                onScrollDirectionChanged = onScrollDirectionChanged,
                onReaderBridgeReady = onReaderBridgeReady,
                onAnnotationTap = onAnnotationTap,
                onInlineLinkTap = onInlineLinkTap,
                onInlineMentionTap = onInlineMentionTap,
            )
        is YabaWebFeature.Editor ->
            YabaEditorFeatureHost(
                modifier = modifier,
                baseUrl = baseUrl,
                feature = f,
                onHostEvent = onHostEvent,
                onUrlClick = onUrlClick,
                onEditorBridgeReady = onEditorBridgeReady,
                onAnnotationTap = onAnnotationTap,
                onMathTap = onMathTap,
                onInlineLinkTap = onInlineLinkTap,
                onInlineMentionTap = onInlineMentionTap,
            )
        is YabaWebFeature.Canvas ->
            YabaCanvasFeatureHost(
                modifier = modifier,
                baseUrl = baseUrl,
                feature = f,
                onHostEvent = onHostEvent,
                onUrlClick = onUrlClick,
                onCanvasBridgeReady = onCanvasBridgeReady,
            )
        is YabaWebFeature.HtmlConverter ->
            YabaHtmlConverterFeatureHost(
                modifier = modifier,
                baseUrl = baseUrl,
                feature = f,
                onHostEvent = onHostEvent,
            )
        is YabaWebFeature.PdfExtractor ->
            YabaPdfExtractorFeatureHost(
                modifier = modifier,
                baseUrl = baseUrl,
                feature = f,
                onHostEvent = onHostEvent,
            )
        is YabaWebFeature.EpubExtractor ->
            YabaEpubExtractorFeatureHost(
                modifier = modifier,
                baseUrl = baseUrl,
                feature = f,
                onHostEvent = onHostEvent,
            )
        is YabaWebFeature.PdfViewer ->
            YabaPdfViewerFeatureHost(
                modifier = modifier,
                baseUrl = baseUrl,
                feature = f,
                onHostEvent = onHostEvent,
                onScrollDirectionChanged = onScrollDirectionChanged,
                onReaderBridgeReady = onReaderBridgeReady,
                onAnnotationTap = onAnnotationTap,
                onInlineLinkTap = onInlineLinkTap,
                onInlineMentionTap = onInlineMentionTap,
            )
        is YabaWebFeature.EpubViewer ->
            YabaEpubViewerFeatureHost(
                modifier = modifier,
                baseUrl = baseUrl,
                feature = f,
                onHostEvent = onHostEvent,
                onScrollDirectionChanged = onScrollDirectionChanged,
                onReaderBridgeReady = onReaderBridgeReady,
                onAnnotationTap = onAnnotationTap,
                onInlineLinkTap = onInlineLinkTap,
                onInlineMentionTap = onInlineMentionTap,
            )
    }
}
