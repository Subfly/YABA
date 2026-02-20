package dev.subfly.yabacore.unfurl

/**
 * Result of readable content extraction, ready for persistence.
 *
 * This is returned by the Unfurler and contains markdown plus downloaded asset bytes.
 * contentVersion and createdAt are set by ReadableContentManager when saving.
 */
data class ReadableUnfurl(
    val markdown: String,
    val title: String?,
    val author: String?,
    val assets: List<ReadableAsset>,
)

/**
 * An asset (image) extracted from the document with its bytes.
 */
data class ReadableAsset(
    /** UUID for the asset */
    val assetId: String,
    /** File extension (e.g., "jpg", "png", "webp") */
    val extension: String,
    /** Raw image bytes */
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ReadableAsset
        return assetId == other.assetId
    }

    override fun hashCode(): Int = assetId.hashCode()
}
