package dev.subfly.yabacore.webview

import androidx.compose.runtime.Stable

/**
 * Input for HTML → reader HTML conversion in the WebView converter shell.
 */
@Stable
data class WebConverterInput(
    val html: String,
    val baseUrl: String?,
)

/**
 * Asset placeholder from converter output (e.g. yaba-asset://N → remote URL).
 */
@Stable
data class WebConverterAsset(
    val placeholder: String,
    val url: String,
    val alt: String? = null,
)

/**
 * Converter bridge result before [dev.subfly.yabacore.unfurl.ConverterResultProcessor] runs.
 */
@Stable
data class WebConverterResult(
    val html: String,
    val assets: List<WebConverterAsset>,
)
