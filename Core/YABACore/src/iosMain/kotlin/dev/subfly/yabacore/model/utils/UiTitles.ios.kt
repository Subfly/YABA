package dev.subfly.yabacore.model.utils

import platform.Foundation.NSBundle

private fun l(key: String): String =
    NSBundle.mainBundle.localizedStringForKey(key = key, value = null, table = null)

actual fun SortType.uiTitle(): String =
    when (this) {
        SortType.CREATED_AT -> l("Sort Created At")
        SortType.EDITED_AT -> l("Sort Edited At")
        SortType.LABEL -> l("Sort Label")
        SortType.CUSTOM -> l("Sort Custom")
    }

actual fun SortOrderType.uiTitle(): String =
    when (this) {
        SortOrderType.ASCENDING -> l("Sort Order Ascending")
        SortOrderType.DESCENDING -> l("Sort Order Descending")
    }

actual fun FabPosition.uiTitle(): String =
    when (this) {
        FabPosition.LEFT -> l("FAB Left Aligned")
        FabPosition.RIGHT -> l("FAB Right Aligned")
        FabPosition.CENTER -> l("FAB Centered")
    }

actual fun ThemePreference.uiTitle(): String =
    when (this) {
        ThemePreference.LIGHT -> l("Theme Light")
        ThemePreference.DARK -> l("Theme Dark")
        ThemePreference.SYSTEM -> l("Theme System")
    }

actual fun ContentAppearance.uiTitle(): String =
    when (this) {
        ContentAppearance.LIST -> l("View List")
        ContentAppearance.CARD -> l("View Card")
        ContentAppearance.GRID -> l("View Grid")
    }

actual fun CardImageSizing.uiTitle(): String =
    when (this) {
        CardImageSizing.BIG -> l("Card Image Sizing Big")
        CardImageSizing.SMALL -> l("Card Image Sizing Small")
    }

actual fun LinkType.uiTitle(): String =
    when (this) {
        LinkType.NONE -> l("Bookmark Type None")
        LinkType.WEB_LINK -> l("Bookmark Type Link")
        LinkType.VIDEO -> l("Bookmark Type Video")
        LinkType.IMAGE -> l("Bookmark Type Image")
        LinkType.AUDIO -> l("Bookmark Type Audio")
        LinkType.MUSIC -> l("Bookmark Type Music")
    }
