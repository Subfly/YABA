package dev.subfly.yabacore.unfurl

data class YabaLinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val host: String?,
    val iconUrl: String?,
    val imageUrl: String?, // kept for backward compatibility
    val videoUrl: String?,
    val iconData: ByteArray?,
    val imageData: ByteArray?, // kept for backward compatibility
    val imageOptions: Map<String, ByteArray>,
    val readableHtml: String?,
)

sealed class UnfurlError(message: String) : Exception(message) {
    data class CannotCreateUrl(val raw: String) :
        UnfurlError("Cannot create url for: $raw")

    data object UnableToUnfurl : UnfurlError("Unable to unfurl url")
}
