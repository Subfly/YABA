package dev.subfly.yabacore.unfurl

/**
 * Result of unfurling a URL.
 *
 * Contains extracted metadata for previews plus structured readable content
 * for the reader view.
 */
data class YabaLinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val host: String?,
    val iconUrl: String?,
    val imageUrl: String?,
    val videoUrl: String?,
    val iconData: ByteArray?,
    val imageData: ByteArray?,
    val imageOptions: Map<String, ByteArray>,
    /** Structured readable content extracted from HTML */
    val readable: ReadableUnfurl?,
)

sealed class UnfurlError(message: String) : Exception(message) {
    data class CannotCreateUrl(val raw: String) :
        UnfurlError("Cannot create url for: $raw")

    data object UnableToUnfurl : UnfurlError("Unable to unfurl url")
}
