package dev.subfly.yabacore.model.utils

enum class ContentAppearance {
    LIST,
    CARD,
    GRID,
}

fun ContentAppearance.uiIconName(): String =
    when (this) {
        ContentAppearance.LIST -> "list-view"
        ContentAppearance.CARD -> "rectangular"
        ContentAppearance.GRID -> "grid-view"
    }
