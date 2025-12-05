package dev.subfly.yabacore.preferences

import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType

/**
 * Canonical representation of user-facing preferences that used to live in Darwin AppStorage.
 *
 * We store enum values as strings (their [name]) to remain forward-compatible while still
 * accepting legacy integer raw values during migration.
 */
data class UserPreferences(
    val hasPassedOnboarding: Boolean = false,
    val hasNamedDevice: Boolean = false,
    val preferredTheme: ThemePreference = ThemePreference.SYSTEM,
    val preferredContentAppearance: ContentAppearance = ContentAppearance.LIST,
    val preferredCardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val preferredCollectionSorting: SortType = SortType.CREATED_AT,
    val preferredBookmarkSorting: SortType = SortType.CREATED_AT,
    val preferredSortOrder: SortOrderType = SortOrderType.ASCENDING,
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

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM,
}

enum class ContentAppearance {
    LIST,
    CARD,
}

enum class CardImageSizing {
    BIG,
    SMALL,
}

enum class FabPosition {
    LEFT,
    RIGHT,
    CENTER,
}

internal inline fun <reified T : Enum<T>> enumFromOrdinal(
    raw: Int,
    default: T,
): T = enumValues<T>().getOrNull(raw) ?: default
