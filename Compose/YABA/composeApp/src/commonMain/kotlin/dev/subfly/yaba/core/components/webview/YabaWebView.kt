package dev.subfly.yaba.core.components.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Mode for YabaWebView: viewer (read-only), editor (editable), or converter (HTML→markdown).
 */
enum class YabaWebViewMode {
    Viewer,
    Editor,
    Converter,
}

/**
 * Input for the converter: HTML string and base URL for resolving relative links.
 */
data class ConverterInput(
    val html: String,
    val baseUrl: String?,
)

/**
 * Result from the converter: markdown plus asset placeholder mappings.
 */
data class ConverterResult(
    val markdown: String,
    val assets: List<ConverterAsset>,
)

/**
 * Asset mapping: placeholder string in markdown (e.g. yaba-asset://0) to remote URL.
 */
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
    onUrlClick: (String) -> Boolean,
    onReady: () -> Unit,
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
 */
@Composable
fun YabaWebViewViewer(
    modifier: Modifier = Modifier,
    baseUrl: String,
    markdown: String = "",
    assetsBaseUrl: String? = null,
    onUrlClick: (String) -> Boolean = { false },
    onReady: () -> Unit = {},
) {
    YabaWebViewViewerInternal(
        modifier = modifier,
        baseUrl = baseUrl,
        markdown = markdown,
        assetsBaseUrl = assetsBaseUrl,
        onUrlClick = onUrlClick,
        onReady = onReady,
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
