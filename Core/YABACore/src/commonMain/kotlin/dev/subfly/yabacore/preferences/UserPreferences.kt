package dev.subfly.yabacore.preferences

import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.ThemePreference

/**
 * Canonical representation of user-facing preferences that used to live in Darwin AppStorage.
 *
 * We store enum values as strings (their [name]) to remain forward-compatible while still accepting
 * legacy integer raw values during migration.
 */
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
    // Announcement toggles (defaults keep announcements hidden unless explicitly reset)
    val announcementsYaba1_2Update: Boolean = true,
    val announcementsYaba1_3Update: Boolean = true,
    val announcementsYaba1_4Update: Boolean = true,
    val announcementsYaba1_5Update: Boolean = true,
    val announcementsCloudKitDrop: Boolean = true,
    val announcementsCloudKitDropUrgent: Boolean = true,
    val announcementsCloudKitDatabaseWipe: Boolean = true,
    val announcementsLegalsUpdate: Boolean = true,
    val announcementsLegalsUpdate2: Boolean = true,
    val migrationCompleted: Boolean = false,
)

internal inline fun <reified T : Enum<T>> enumFromOrdinal(
    raw: Int,
    default: T,
): T = enumValues<T>().getOrNull(raw) ?: default
