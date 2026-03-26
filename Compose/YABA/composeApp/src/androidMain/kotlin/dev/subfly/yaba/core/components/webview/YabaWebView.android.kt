package dev.subfly.yaba.core.components.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.subfly.yabacore.webview.WebViewEditorBridge
import dev.subfly.yabacore.webview.WebViewReaderBridge
import dev.subfly.yabacore.webview.InlineLinkTapEvent
import dev.subfly.yabacore.webview.InlineMentionTapEvent
import dev.subfly.yabacore.webview.MathTapEvent
import dev.subfly.yabacore.webview.YabaWebFeature
import dev.subfly.yabacore.webview.YabaWebHostEvent
import dev.subfly.yabacore.webview.YabaWebScrollDirection

@Composable
actual fun YabaWebViewHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onUrlClick: (String) -> Boolean,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReaderBridgeReady: (WebViewReaderBridge?) -> Unit,
    onEditorBridgeReady: (WebViewEditorBridge?) -> Unit,
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
