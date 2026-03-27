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
 * Link metadata from the WebView bridge (web-meta-scraper + tidy-url on Core-fetched HTML).
 */
@Stable
data class WebLinkMetadata(
    val cleanedUrl: String,
    val title: String? = null,
    val description: String? = null,
    val author: String? = null,
    val date: String? = null,
    val audio: String? = null,
    val video: String? = null,
    val image: String? = null,
    val logo: String? = null,
)

/**
 * Converter bridge result before [dev.subfly.yabacore.unfurl.ConverterResultProcessor] runs.
 */
@Stable
data class WebConverterResult(
    val documentJson: String,
    val assets: List<WebConverterAsset>,
    val linkMetadata: WebLinkMetadata,
)
