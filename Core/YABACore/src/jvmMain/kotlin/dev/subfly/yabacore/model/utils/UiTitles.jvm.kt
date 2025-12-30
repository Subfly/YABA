package dev.subfly.yabacore.model.utils

actual fun SortType.uiTitle(): String =
    when (this) {
        SortType.CREATED_AT -> "Sort Created At"
        SortType.EDITED_AT -> "Sort Edited At"
        SortType.LABEL -> "Sort Label"
        SortType.CUSTOM -> "Sort Custom"
    }

actual fun SortOrderType.uiTitle(): String =
    when (this) {
        SortOrderType.ASCENDING -> "Sort Order Ascending"
        SortOrderType.DESCENDING -> "Sort Order Descending"
    }

actual fun FabPosition.uiTitle(): String =
    when (this) {
        FabPosition.LEFT -> "FAB Left Aligned"
        FabPosition.RIGHT -> "FAB Right Aligned"
        FabPosition.CENTER -> "FAB Centered"
    }

actual fun ThemePreference.uiTitle(): String =
    when (this) {
        ThemePreference.LIGHT -> "Theme Light"
        ThemePreference.DARK -> "Theme Dark"
        ThemePreference.SYSTEM -> "Theme System"
    }

actual fun ContentAppearance.uiTitle(): String =
    when (this) {
        ContentAppearance.LIST -> "View List"
        ContentAppearance.CARD -> "View Card"
        ContentAppearance.GRID -> "View Grid"
    }

actual fun CardImageSizing.uiTitle(): String =
    when (this) {
        CardImageSizing.BIG -> "Card Image Sizing Big"
        CardImageSizing.SMALL -> "Card Image Sizing Small"
    }

actual fun LinkType.uiTitle(): String =
    when (this) {
        LinkType.NONE -> "Bookmark Type None"
        LinkType.WEB_LINK -> "Bookmark Type Link"
        LinkType.VIDEO -> "Bookmark Type Video"
        LinkType.IMAGE -> "Bookmark Type Image"
        LinkType.AUDIO -> "Bookmark Type Audio"
        LinkType.MUSIC -> "Bookmark Type Music"
    }
