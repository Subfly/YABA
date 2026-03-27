package dev.subfly.yabacore.unfurl

/**
 * Result of fetching a link page for preview and reader conversion.
 *
 * HTML parsing, URL cleanup, and structured metadata extraction run in the WebView
 * converter. [Unfurler] only performs the HTTP fetch and supplies [rawHtml] to the bridge.
 */
data class YabaLinkPreview(
    val url: String,
    val host: String?,
    val rawHtml: String?,
    /** Structured readable content extracted from HTML (legacy path; null when using converter). */
    val readable: ReadableUnfurl?,
)

sealed class UnfurlError(message: String) : Exception(message) {
    data class CannotCreateUrl(val raw: String) :
        UnfurlError("Cannot create url for: $raw")

    data object UnableToUnfurl : UnfurlError("Unable to unfurl url")
}
