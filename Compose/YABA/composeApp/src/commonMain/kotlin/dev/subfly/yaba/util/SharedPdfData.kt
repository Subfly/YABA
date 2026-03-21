package dev.subfly.yaba.util

/**
 * PDF data passed from share intent to docmark creation.
 */
data class SharedPdfData(
    val bytes: ByteArray,
    val sourceFileName: String?,
) {
    override fun equals(other: Any?): Boolean =
        other is SharedPdfData &&
                bytes.contentEquals(other.bytes) &&
                sourceFileName == other.sourceFileName

    override fun hashCode(): Int = bytes.contentHashCode() * 31 + (sourceFileName?.hashCode() ?: 0)
}
