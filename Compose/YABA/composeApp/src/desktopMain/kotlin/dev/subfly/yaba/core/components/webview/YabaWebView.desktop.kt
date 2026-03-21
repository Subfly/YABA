package dev.subfly.yaba.core.components.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.subfly.yabacore.webview.WebViewEditorBridge
import dev.subfly.yabacore.webview.WebViewReaderBridge
import dev.subfly.yabacore.webview.YabaWebFeature
import dev.subfly.yabacore.webview.YabaWebHostEvent
import dev.subfly.yabacore.webview.YabaWebScrollDirection

/**
 * Desktop stub: WebView integration is not implemented yet.
 */
@Composable
actual fun YabaWebViewHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onUrlClick: (String) -> Boolean,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onBridgeReady: (WebViewReaderBridge?) -> Unit,
    onEditorBridgeReady: (WebViewEditorBridge?) -> Unit,
    onHighlightTap: (String) -> Unit,
) {
    val showPlaceholder = feature is YabaWebFeature.MarkdownViewer ||
        feature is YabaWebFeature.PdfViewer ||
        feature is YabaWebFeature.Editor

    if (showPlaceholder) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "WebView not available on desktop yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    LaunchedEffect(feature) {
        onBridgeReady(null)
        onEditorBridgeReady(null)
        when (val f = feature) {
            is YabaWebFeature.HtmlConverter ->
                if (f.input != null) {
                    onHostEvent(
                        YabaWebHostEvent.HtmlConverterFailure(
                            UnsupportedOperationException("WebView not available on desktop"),
                        ),
                    )
                }
            is YabaWebFeature.PdfExtractor ->
                if (f.input != null) {
                    onHostEvent(
                        YabaWebHostEvent.PdfConverterFailure(
                            UnsupportedOperationException("WebView not available on desktop"),
                        ),
                    )
                }
            else -> Unit
        }
    }
}
