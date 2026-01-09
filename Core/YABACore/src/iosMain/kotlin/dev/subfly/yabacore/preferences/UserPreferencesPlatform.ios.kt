package dev.subfly.yabacore.preferences

import androidx.datastore.preferences.core.edit
import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.CollectionAppearance
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.ThemePreference
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import kotlinx.coroutines.flow.first
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults

private const val APP_GROUP_ID = "group.dev.subfly.YABA"

internal actual fun resolveUserSettingsDataStoreFile(
        subdir: String,
        fileName: String,
): PlatformFile {
    val fileManager = NSFileManager.defaultManager
    val appGroupUrl = fileManager.containerURLForSecurityApplicationGroupIdentifier(APP_GROUP_ID)
    val rootPath = appGroupUrl?.path ?: FileKit.filesDir.absolutePath()
    val root = PlatformFile(rootPath)
    val settingsDir = (root / subdir).also { it.createDirectories() }
    return settingsDir / fileName
}

internal actual suspend fun migratePlatformPreferencesIfNeeded(store: UserPreferencesStore) {
    val dataStore = store.dataStoreHandle
    val current = dataStore.data.first()
    if (current[UserPreferenceKeys.migrationCompleted] == true) return

    val standard = NSUserDefaults.standardUserDefaults
    val appGroup = NSUserDefaults(suiteName = APP_GROUP_ID)

    dataStore.edit { prefs ->
        prefs[UserPreferenceKeys.hasPassedOnboarding] =
                standard.boolOrDefault(CoreConstants.Settings.HAS_PASSED_ONBOARDING, false)
        prefs[UserPreferenceKeys.hasNamedDevice] =
                standard.boolOrDefault(CoreConstants.Settings.HAS_NAMED_DEVICE, false)

        prefs[UserPreferenceKeys.preferredTheme] =
                enumFromOrdinal(
                                standard.intOrDefault(
                                        CoreConstants.Settings.PREFERRED_THEME,
                                        ThemePreference.SYSTEM.ordinal
                                ),
                                ThemePreference.SYSTEM,
                        )
                        .name

        @Suppress("DEPRECATION")
        prefs[UserPreferenceKeys.preferredContentAppearance] =
                enumFromOrdinal(
                                standard.intOrDefault(
                                        CoreConstants.Settings.PREFERRED_CONTENT_APPEARANCE,
                                        ContentAppearance.LIST.ordinal,
                                ),
                                ContentAppearance.LIST,
                        )
                        .name

        // For Apple migration: collections always use LIST, bookmarks inherit the old appearance
        prefs[UserPreferenceKeys.preferredCollectionAppearance] = CollectionAppearance.LIST.name

        // Map old ContentAppearance to new BookmarkAppearance
        @Suppress("DEPRECATION")
        val oldAppearanceOrdinal =
                standard.intOrDefault(
                        CoreConstants.Settings.PREFERRED_CONTENT_APPEARANCE,
                        ContentAppearance.LIST.ordinal,
                )
        val bookmarkAppearance =
                when (enumFromOrdinal(oldAppearanceOrdinal, ContentAppearance.LIST)) {
                    ContentAppearance.LIST -> BookmarkAppearance.LIST
                    ContentAppearance.CARD -> BookmarkAppearance.CARD
                    ContentAppearance.GRID -> BookmarkAppearance.GRID
                }
        prefs[UserPreferenceKeys.preferredBookmarkAppearance] = bookmarkAppearance.name

        prefs[UserPreferenceKeys.preferredCardImageSizing] =
                enumFromOrdinal(
                                standard.intOrDefault(
                                        CoreConstants.Settings.PREFERRED_CARD_IMAGE_SIZING,
                                        CardImageSizing.SMALL.ordinal,
                                ),
                                CardImageSizing.SMALL,
                        )
                        .name

        prefs[UserPreferenceKeys.preferredCollectionSorting] =
                enumFromOrdinal(
                                standard.intOrDefault(
                                        CoreConstants.Settings.PREFERRED_COLLECTION_SORTING,
                                        SortType.CREATED_AT.ordinal,
                                ),
                                SortType.CREATED_AT,
                        )
                        .name

        prefs[UserPreferenceKeys.preferredBookmarkSorting] =
                enumFromOrdinal(
                                standard.intOrDefault(
                                        CoreConstants.Settings.PREFERRED_BOOKMARK_SORTING,
                                        SortType.CREATED_AT.ordinal,
                                ),
                                SortType.CREATED_AT,
                        )
                        .name

        prefs[UserPreferenceKeys.preferredSortOrder] =
                enumFromOrdinal(
                                standard.intOrDefault(
                                        CoreConstants.Settings.PREFERRED_SORT_ORDER,
                                        SortOrderType.ASCENDING.ordinal,
                                ),
                                SortOrderType.ASCENDING,
                        )
                        .name

        prefs[UserPreferenceKeys.preferredFabPosition] =
                enumFromOrdinal(
                                standard.intOrDefault(
                                        CoreConstants.Settings.PREFERRED_FAB_POSITION,
                                        FabPosition.CENTER.ordinal,
                                ),
                                FabPosition.CENTER,
                        )
                        .name

        prefs[UserPreferenceKeys.disableBackgroundAnimation] =
                standard.boolOrDefault(CoreConstants.Settings.DISABLE_BACKGROUND_ANIMATION, false)

        prefs[UserPreferenceKeys.deviceId] =
                standard.stringOrDefault(CoreConstants.Settings.DEVICE_ID, "")

        prefs[UserPreferenceKeys.deviceName] =
                standard.stringOrDefault(CoreConstants.Settings.DEVICE_NAME, "")

        prefs[UserPreferenceKeys.showRecents] =
                standard.boolOrDefault(CoreConstants.Settings.SHOW_RECENTS, true)

        prefs[UserPreferenceKeys.showMenuBarItem] =
                standard.boolOrDefault(CoreConstants.Settings.SHOW_MENU_BAR_ITEM, true)

        val simplifiedShare =
                appGroup?.boolOrDefault(
                        CoreConstants.Settings.USE_SIMPLIFIED_SHARE,
                        false,
                )
                        ?: standard.boolOrDefault(
                                CoreConstants.Settings.USE_SIMPLIFIED_SHARE,
                                false
                        )
        prefs[UserPreferenceKeys.useSimplifiedShare] = simplifiedShare

        prefs[UserPreferenceKeys.preventDeletionSync] =
                standard.boolOrDefault(CoreConstants.Settings.PREVENT_DELETION_SYNC, false)

        prefs[UserPreferenceKeys.announcementsYaba12] =
                standard.boolOrDefault(CoreConstants.Announcements.YABA_1_2_UPDATE, true)
        prefs[UserPreferenceKeys.announcementsYaba13] =
                standard.boolOrDefault(CoreConstants.Announcements.YABA_1_3_UPDATE, true)
        prefs[UserPreferenceKeys.announcementsYaba14] =
                standard.boolOrDefault(CoreConstants.Announcements.YABA_1_4_UPDATE, true)
        prefs[UserPreferenceKeys.announcementsYaba15] =
                standard.boolOrDefault(CoreConstants.Announcements.YABA_1_5_UPDATE, true)
        prefs[UserPreferenceKeys.announcementsCloudKitDrop] =
                standard.boolOrDefault(CoreConstants.Announcements.CLOUDKIT_DROP, true)
        prefs[UserPreferenceKeys.announcementsCloudKitDropUrgent] =
                standard.boolOrDefault(CoreConstants.Announcements.CLOUDKIT_DROP_URGENT, true)
        prefs[UserPreferenceKeys.announcementsCloudKitDatabaseWipe] =
                standard.boolOrDefault(CoreConstants.Announcements.CLOUDKIT_DATABASE_WIPE, true)
        prefs[UserPreferenceKeys.announcementsLegalsUpdate] =
                standard.boolOrDefault(CoreConstants.Announcements.LEGALS_UPDATE, true)
        prefs[UserPreferenceKeys.announcementsLegalsUpdate2] =
                standard.boolOrDefault(CoreConstants.Announcements.LEGALS_UPDATE_2, true)

        prefs[UserPreferenceKeys.migrationCompleted] = true
    }
}

private fun NSUserDefaults.boolOrDefault(key: String, default: Boolean): Boolean =
        if (objectForKey(key) != null) boolForKey(key) else default

private fun NSUserDefaults.intOrDefault(key: String, default: Int): Int =
        if (objectForKey(key) != null) integerForKey(key).toInt() else default

private fun NSUserDefaults.stringOrDefault(key: String, default: String): String =
        stringForKey(key) ?: default
