package dev.subfly.yabacore.model.utils

/**
 * Stored on [dev.subfly.yabacore.database.entities.DocBookmarkEntity] for FILE bookmarks.
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
