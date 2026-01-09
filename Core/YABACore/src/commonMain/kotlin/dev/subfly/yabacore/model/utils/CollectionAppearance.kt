package dev.subfly.yabacore.model.utils

/**
 * Appearance options for collection items (Folders, Tags).
 * Collections support either LIST or GRID view.
 */
enum class CollectionAppearance {
    LIST,
    GRID,
}

fun CollectionAppearance.uiIconName(): String =
    when (this) {
        CollectionAppearance.LIST -> "list-view"
        CollectionAppearance.GRID -> "grid-view"
    }

expect fun CollectionAppearance.uiTitle(): String

