package dev.subfly.yaba.core.components.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.utils.ReaderPreferences

enum class YabaWebPlatform {
    Compose,
    Darwin,
}

enum class YabaWebAppearance {
    Auto,
    Light,
    Dark,
}

enum class YabaWebScrollDirection {
    Up,
    Down,
}

/**
 * Input for the converter: HTML string and base URL for resolving relative links.
 */
@Stable
data class ConverterInput(
    val html: String,
    val baseUrl: String?,
)

/**
 * Result from the converter: markdown plus asset placeholder mappings.
 */
@Stable
data class ConverterResult(
    val markdown: String,
    val assets: List<ConverterAsset>,
)

/**
 * Asset mapping: placeholder string in markdown (e.g. yaba-asset://0) to remote URL.
 */
@Stable
data class ConverterAsset(
    val placeholder: String,
    val url: String,
    val alt: String? = null,
)

/**
 * Internal expect-actual: mode-specific WebView implementations.
 */
@Composable
internal expect fun YabaWebViewViewerInternal(
    modifier: Modifier,
    baseUrl: String,
    markdown: String,
    assetsBaseUrl: String?,
    platform: YabaWebPlatform,
    appearance: YabaWebAppearance,
    readerPreferences: ReaderPreferences,
    onUrlClick: (String) -> Boolean,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReady: () -> Unit,
    onBridgeReady: (WebViewReaderBridge) -> Unit,
    onHighlightTap: (String) -> Unit,
    highlights: List<HighlightUiModel>,
)

@Composable
internal expect fun YabaWebViewEditorInternal(
    modifier: Modifier,
    baseUrl: String,
    markdown: String,
    assetsBaseUrl: String?,
    onUrlClick: (String) -> Boolean,
    onReady: () -> Unit,
)

@Composable
internal expect fun YabaWebViewConverterInternal(
    modifier: Modifier,
    baseUrl: String,
    input: ConverterInput?,
    onConverterResult: (ConverterResult) -> Unit,
    onConverterError: (Throwable) -> Unit,
    onReady: () -> Unit,
)

/**
 * Viewer: displays markdown read-only. Links open via [onUrlClick].
 *
 * @param onBridgeReady Called when the viewer is ready; provides a bridge for
 *   [WebViewReaderBridge.getSelectionSnapshot] and [WebViewReaderBridge.setHighlights].
 * @param onHighlightTap Called when user taps a highlight; opens edit sheet.
 * @param highlights Saved highlights to render in the viewer.
 */
@Composable
fun YabaWebViewViewer(
    modifier: Modifier = Modifier,
    baseUrl: String,
    markdown: String = "",
    assetsBaseUrl: String? = null,
    platform: YabaWebPlatform = YabaWebPlatform.Compose,
    appearance: YabaWebAppearance = YabaWebAppearance.Auto,
    readerPreferences: ReaderPreferences = ReaderPreferences(),
    onUrlClick: (String) -> Boolean = { false },
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit = {},
    onReady: () -> Unit = {},
    onBridgeReady: (WebViewReaderBridge) -> Unit = {},
    onHighlightTap: (String) -> Unit = {},
    highlights: List<HighlightUiModel> = emptyList(),
) {
    YabaWebViewViewerInternal(
        modifier = modifier,
        baseUrl = baseUrl,
        markdown = markdown,
        assetsBaseUrl = assetsBaseUrl,
        platform = platform,
        appearance = appearance,
        readerPreferences = readerPreferences,
        onUrlClick = onUrlClick,
        onScrollDirectionChanged = onScrollDirectionChanged,
        onReady = onReady,
        onBridgeReady = onBridgeReady,
        onHighlightTap = onHighlightTap,
        highlights = highlights,
    )
}

/**
 * Editor: editable markdown. Uses editor.html.
 */
@Composable
fun YabaWebViewEditor(
    modifier: Modifier = Modifier,
    baseUrl: String,
    initialMarkdown: String = "",
    assetsBaseUrl: String? = null,
    onUrlClick: (String) -> Boolean = { false },
    onReady: () -> Unit = {},
) {
    YabaWebViewEditorInternal(
        modifier = modifier,
        baseUrl = baseUrl,
        markdown = initialMarkdown,
        assetsBaseUrl = assetsBaseUrl,
        onUrlClick = onUrlClick,
        onReady = onReady,
    )
}

/**
 * Converter: runs HTML→markdown. Call with [input]; [onConverterResult] or [onConverterError] when done.
 */
@Composable
fun YabaWebViewConverter(
    modifier: Modifier = Modifier,
    baseUrl: String,
    input: ConverterInput?,
    onConverterResult: (ConverterResult) -> Unit,
    onConverterError: (Throwable) -> Unit = {},
    onReady: () -> Unit = {},
) {
    YabaWebViewConverterInternal(
        modifier = modifier,
        baseUrl = baseUrl,
        input = input,
        onConverterResult = onConverterResult,
        onConverterError = onConverterError,
        onReady = onReady,
    )
}
