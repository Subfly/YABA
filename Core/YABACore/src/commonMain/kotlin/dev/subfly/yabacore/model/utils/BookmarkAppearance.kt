package dev.subfly.yabacore.model.utils

/**
 * Appearance options for bookmark items.
 * Bookmarks support LIST, CARD (with image sizing options), or GRID view.
 */
enum class BookmarkAppearance {
    LIST,
    CARD,
    GRID,
}

fun BookmarkAppearance.uiIconName(): String =
    when (this) {
        BookmarkAppearance.LIST -> "list-view"
        BookmarkAppearance.CARD -> "rectangular"
        BookmarkAppearance.GRID -> "grid-view"
    }

expect fun BookmarkAppearance.uiTitle(): String

