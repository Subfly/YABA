package dev.subfly.yaba.core.preferences

import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.FabPosition
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType
import dev.subfly.yaba.core.model.utils.ThemePreference
import kotlinx.serialization.Serializable

/**
 * Canonical representation of user-facing preferences that used to live in Darwin AppStorage.
 *
 * We store enum values as strings (their [name]) for forward-compatible serialization.
 */
@Serializable
data class UserPreferences(
    val hasPassedOnboarding: Boolean = false,
    val hasNamedDevice: Boolean = false,
    val preferredTheme: ThemePreference = ThemePreference.SYSTEM,
    val preferredBookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val preferredCardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val preferredCollectionSorting: SortType = SortType.CREATED_AT,
    val preferredCollectionSortOrder: SortOrderType = SortOrderType.ASCENDING,
    val preferredBookmarkSorting: SortType = SortType.CREATED_AT,
    val preferredBookmarkSortOrder: SortOrderType = SortOrderType.DESCENDING,
    val preferredFabPosition: FabPosition = FabPosition.CENTER,
    val disableBackgroundAnimation: Boolean = false,
    val deviceId: String = "",
    val deviceName: String = "",
    val showRecents: Boolean = true,
    val showMenuBarItem: Boolean = true,
    val useSimplifiedShare: Boolean = false,
    val preventDeletionSync: Boolean = false,
    val announcementsYaba1_2Update: Boolean = true,
    val announcementsYaba1_3Update: Boolean = true,
    val announcementsYaba1_4Update: Boolean = true,
    val announcementsYaba1_5Update: Boolean = true,
    val announcementsCloudKitDrop: Boolean = true,
    val announcementsCloudKitDropUrgent: Boolean = true,
    val announcementsCloudKitDatabaseWipe: Boolean = true,
    val announcementsLegalsUpdate: Boolean = true,
    val announcementsLegalsUpdate2: Boolean = true,
    val privateBookmarkPasswordHash: String = "",
)

internal inline fun <reified T : Enum<T>> enumFromOrdinal(
    raw: Int,
    default: T,
): T = enumValues<T>().getOrNull(raw) ?: default
