package dev.subfly.yaba.util

import dev.subfly.yaba.core.model.utils.DocmarkType

/**
 * Document bytes passed from share intent to docmark creation (PDF or EPUB).
 */
data class SharedDocumentData(
    val bytes: ByteArray,
    val sourceFileName: String?,
    val docmarkType: DocmarkType,
) {
    override fun equals(other: Any?): Boolean =
        other is SharedDocumentData &&
            bytes.contentEquals(other.bytes) &&
            sourceFileName == other.sourceFileName &&
            docmarkType == other.docmarkType

    override fun hashCode(): Int =
        bytes.contentHashCode() * 31 + (sourceFileName?.hashCode() ?: 0) + docmarkType.hashCode()
}
