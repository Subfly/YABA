package dev.subfly.yaba.core.components.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Composable
internal expect fun YabaPdfWebViewViewerInternal(
    modifier: Modifier,
    baseUrl: String,
    pdfUrl: String,
    platform: YabaWebPlatform,
    appearance: YabaWebAppearance,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReady: () -> Unit,
    onBridgeReady: (WebViewReaderBridge) -> Unit,
    onHighlightTap: (String) -> Unit,
    highlights: List<HighlightUiModel>,
)

@Composable
fun YabaPdfWebViewViewer(
    modifier: Modifier = Modifier,
    baseUrl: String,
    pdfUrl: String,
    platform: YabaWebPlatform = YabaWebPlatform.Compose,
    appearance: YabaWebAppearance = YabaWebAppearance.Auto,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit = {},
    onReady: () -> Unit = {},
    onBridgeReady: (WebViewReaderBridge) -> Unit = {},
    onHighlightTap: (String) -> Unit = {},
    highlights: List<HighlightUiModel> = emptyList(),
) {
    YabaPdfWebViewViewerInternal(
        modifier = modifier,
        baseUrl = baseUrl,
        pdfUrl = pdfUrl,
        platform = platform,
        appearance = appearance,
        onScrollDirectionChanged = onScrollDirectionChanged,
        onReady = onReady,
        onBridgeReady = onBridgeReady,
        onHighlightTap = onHighlightTap,
        highlights = highlights,
    )
}
