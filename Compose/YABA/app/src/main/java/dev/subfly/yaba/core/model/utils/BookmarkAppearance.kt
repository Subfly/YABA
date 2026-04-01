package dev.subfly.yaba.core.model.utils

import kotlinx.serialization.Serializable

/**
 * Appearance options for bookmark items.
 * Bookmarks support LIST, CARD (with image sizing options), or GRID view.
 */
@Serializable
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

