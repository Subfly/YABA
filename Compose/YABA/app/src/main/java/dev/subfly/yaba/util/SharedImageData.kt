package dev.subfly.yaba.util

/**
 * Image data passed from share extension to Imagemark creation.
 */
data class SharedImageData(
    val bytes: ByteArray,
    val extension: String,
) {
    override fun equals(other: Any?): Boolean =
        other is SharedImageData && bytes.contentEquals(other.bytes) && extension == other.extension

    override fun hashCode(): Int = bytes.contentHashCode() * 31 + extension.hashCode()
}
