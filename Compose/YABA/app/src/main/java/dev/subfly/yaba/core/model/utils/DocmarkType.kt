package dev.subfly.yaba.core.model.utils

/**
 * Stored on [dev.subfly.yaba.core.database.entities.DocBookmarkEntity] for FILE bookmarks.
 */
enum class DocmarkType {
    PDF,
    EPUB;

    companion object {
        fun fromFileExtension(extension: String): DocmarkType? =
            when (extension.lowercase().removePrefix(".")) {
                "pdf" -> PDF
                "epub" -> EPUB
                else -> null
            }
    }
}
