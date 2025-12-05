@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package dev.subfly.yabacore.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.ThemePreference
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath

private const val SETTINGS_SUBDIR = "settings"
private const val SETTINGS_FILE = "user_settings.preferences_pb"
private const val MIGRATION_FLAG = "user_settings_migration_completed"

internal object UserPreferenceKeys {
    val hasPassedOnboarding = booleanPreferencesKey(CoreConstants.Settings.HAS_PASSED_ONBOARDING)
    val hasNamedDevice = booleanPreferencesKey(CoreConstants.Settings.HAS_NAMED_DEVICE)
    val preferredTheme = stringPreferencesKey(CoreConstants.Settings.PREFERRED_THEME)
    val preferredContentAppearance =
        stringPreferencesKey(CoreConstants.Settings.PREFERRED_CONTENT_APPEARANCE)
    val preferredCardImageSizing =
        stringPreferencesKey(CoreConstants.Settings.PREFERRED_CARD_IMAGE_SIZING)
    val preferredCollectionSorting =
        stringPreferencesKey(CoreConstants.Settings.PREFERRED_COLLECTION_SORTING)
    val preferredBookmarkSorting =
        stringPreferencesKey(CoreConstants.Settings.PREFERRED_BOOKMARK_SORTING)
    val preferredSortOrder = stringPreferencesKey(CoreConstants.Settings.PREFERRED_SORT_ORDER)
    val preferredFabPosition = stringPreferencesKey(CoreConstants.Settings.PREFERRED_FAB_POSITION)
    val disableBackgroundAnimation =
        booleanPreferencesKey(CoreConstants.Settings.DISABLE_BACKGROUND_ANIMATION)
    val deviceId = stringPreferencesKey(CoreConstants.Settings.DEVICE_ID)
    val deviceName = stringPreferencesKey(CoreConstants.Settings.DEVICE_NAME)
    val showRecents = booleanPreferencesKey(CoreConstants.Settings.SHOW_RECENTS)
    val showMenuBarItem = booleanPreferencesKey(CoreConstants.Settings.SHOW_MENU_BAR_ITEM)
    val useSimplifiedShare = booleanPreferencesKey(CoreConstants.Settings.USE_SIMPLIFIED_SHARE)
    val preventDeletionSync = booleanPreferencesKey(CoreConstants.Settings.PREVENT_DELETION_SYNC)

    val announcementsYaba12 = booleanPreferencesKey(CoreConstants.Announcements.YABA_1_2_UPDATE)
    val announcementsYaba13 = booleanPreferencesKey(CoreConstants.Announcements.YABA_1_3_UPDATE)
    val announcementsYaba14 = booleanPreferencesKey(CoreConstants.Announcements.YABA_1_4_UPDATE)
    val announcementsYaba15 = booleanPreferencesKey(CoreConstants.Announcements.YABA_1_5_UPDATE)
    val announcementsCloudKitDrop = booleanPreferencesKey(CoreConstants.Announcements.CLOUDKIT_DROP)
    val announcementsCloudKitDropUrgent =
        booleanPreferencesKey(CoreConstants.Announcements.CLOUDKIT_DROP_URGENT)
    val announcementsCloudKitDatabaseWipe =
        booleanPreferencesKey(CoreConstants.Announcements.CLOUDKIT_DATABASE_WIPE)
    val announcementsLegalsUpdate = booleanPreferencesKey(CoreConstants.Announcements.LEGALS_UPDATE)
    val announcementsLegalsUpdate2 =
        booleanPreferencesKey(CoreConstants.Announcements.LEGALS_UPDATE_2)

    val migrationCompleted = booleanPreferencesKey(MIGRATION_FLAG)
}

class UserPreferencesStore internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    internal val dataStoreHandle: DataStore<Preferences> = dataStore

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { it.toUserPreferences() }

    suspend fun get(): UserPreferences = dataStore.data.first().toUserPreferences()

    suspend fun setHasPassedOnboarding(value: Boolean) =
        setBoolean(UserPreferenceKeys.hasPassedOnboarding, value)

    suspend fun setHasNamedDevice(value: Boolean) =
        setBoolean(UserPreferenceKeys.hasNamedDevice, value)

    suspend fun setPreferredTheme(ordinal: Int) =
        setPreferredTheme(enumFromOrdinal(ordinal, ThemePreference.SYSTEM))

    suspend fun setPreferredTheme(value: ThemePreference) =
        setEnum(UserPreferenceKeys.preferredTheme, value)

    suspend fun setPreferredContentAppearance(ordinal: Int) =
        setPreferredContentAppearance(enumFromOrdinal(ordinal, ContentAppearance.LIST))

    suspend fun setPreferredContentAppearance(value: ContentAppearance) =
        setEnum(UserPreferenceKeys.preferredContentAppearance, value)

    suspend fun setPreferredCardImageSizing(ordinal: Int) =
        setPreferredCardImageSizing(enumFromOrdinal(ordinal, CardImageSizing.SMALL))

    suspend fun setPreferredCardImageSizing(value: CardImageSizing) =
        setEnum(UserPreferenceKeys.preferredCardImageSizing, value)

    suspend fun setPreferredCollectionSorting(ordinal: Int) =
        setPreferredCollectionSorting(enumFromOrdinal(ordinal, SortType.CREATED_AT))

    suspend fun setPreferredCollectionSorting(value: SortType) =
        setEnum(UserPreferenceKeys.preferredCollectionSorting, value)

    suspend fun setPreferredBookmarkSorting(ordinal: Int) =
        setPreferredBookmarkSorting(enumFromOrdinal(ordinal, SortType.CREATED_AT))

    suspend fun setPreferredBookmarkSorting(value: SortType) =
        setEnum(UserPreferenceKeys.preferredBookmarkSorting, value)

    suspend fun setPreferredSortOrder(ordinal: Int) =
        setPreferredSortOrder(enumFromOrdinal(ordinal, SortOrderType.ASCENDING))

    suspend fun setPreferredSortOrder(value: SortOrderType) =
        setEnum(UserPreferenceKeys.preferredSortOrder, value)

    suspend fun setPreferredFabPosition(ordinal: Int) =
        setPreferredFabPosition(enumFromOrdinal(ordinal, FabPosition.CENTER))

    suspend fun setPreferredFabPosition(value: FabPosition) =
        setEnum(UserPreferenceKeys.preferredFabPosition, value)

    suspend fun setDisableBackgroundAnimation(value: Boolean) =
        setBoolean(UserPreferenceKeys.disableBackgroundAnimation, value)

    suspend fun setDeviceId(value: String) = setString(UserPreferenceKeys.deviceId, value)
    suspend fun setDeviceName(value: String) = setString(UserPreferenceKeys.deviceName, value)
    suspend fun setShowRecents(value: Boolean) = setBoolean(UserPreferenceKeys.showRecents, value)
    suspend fun setShowMenuBarItem(value: Boolean) =
        setBoolean(UserPreferenceKeys.showMenuBarItem, value)

    suspend fun setUseSimplifiedShare(value: Boolean) =
        setBoolean(UserPreferenceKeys.useSimplifiedShare, value)

    suspend fun setPreventDeletionSync(value: Boolean) =
        setBoolean(UserPreferenceKeys.preventDeletionSync, value)

    suspend fun setAnnouncementsYaba12(value: Boolean) =
        setBoolean(UserPreferenceKeys.announcementsYaba12, value)

    suspend fun setAnnouncementsYaba13(value: Boolean) =
        setBoolean(UserPreferenceKeys.announcementsYaba13, value)

    suspend fun setAnnouncementsYaba14(value: Boolean) =
        setBoolean(UserPreferenceKeys.announcementsYaba14, value)

    suspend fun setAnnouncementsYaba15(value: Boolean) =
        setBoolean(UserPreferenceKeys.announcementsYaba15, value)

    suspend fun setAnnouncementsCloudKitDrop(value: Boolean) =
        setBoolean(UserPreferenceKeys.announcementsCloudKitDrop, value)

    suspend fun setAnnouncementsCloudKitDropUrgent(value: Boolean) =
        setBoolean(UserPreferenceKeys.announcementsCloudKitDropUrgent, value)

    suspend fun setAnnouncementsCloudKitDatabaseWipe(value: Boolean) =
        setBoolean(UserPreferenceKeys.announcementsCloudKitDatabaseWipe, value)

    suspend fun setAnnouncementsLegalsUpdate(value: Boolean) =
        setBoolean(UserPreferenceKeys.announcementsLegalsUpdate, value)

    suspend fun setAnnouncementsLegalsUpdate2(value: Boolean) =
        setBoolean(UserPreferenceKeys.announcementsLegalsUpdate2, value)

    suspend fun markMigrationComplete() =
        setBoolean(UserPreferenceKeys.migrationCompleted, true)

    @OptIn(ExperimentalUuidApi::class)
    suspend fun ensureDefaults() {
        dataStore.edit { prefs ->
            if (!prefs.contains(UserPreferenceKeys.deviceId)) {
                prefs[UserPreferenceKeys.deviceId] = Uuid.random().toString()
            }
        }
    }

    private suspend fun setBoolean(
        key: Preferences.Key<Boolean>,
        value: Boolean,
    ) = dataStore.edit { prefs -> prefs[key] = value }

    private suspend fun setString(
        key: Preferences.Key<String>,
        value: String,
    ) = dataStore.edit { prefs -> prefs[key] = value }

    private suspend fun <T : Enum<T>> setEnum(
        key: Preferences.Key<String>,
        value: T,
    ) = dataStore.edit { prefs -> prefs[key] = value.name }
}

object UserPreferencesStoreFactory {
    fun create(
        coroutineContext: CoroutineContext = Dispatchers.Default,
    ): UserPreferencesStore {
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = CoroutineScope(SupervisorJob() + coroutineContext),
            produceFile = {
                resolveUserSettingsDataStoreFile(
                    SETTINGS_SUBDIR,
                    SETTINGS_FILE,
                ).absolutePath().toPath()
            },
        )

        return UserPreferencesStore(dataStore).also { store ->
            runBlocking { store.ensureDefaults() }
        }
    }
}

object SettingsStores {
    val userPreferences: UserPreferencesStore by lazy { UserPreferencesStoreFactory.create() }
}

private fun Preferences.toUserPreferences(): UserPreferences =
    UserPreferences(
        hasPassedOnboarding = getBoolean(UserPreferenceKeys.hasPassedOnboarding, false),
        hasNamedDevice = getBoolean(UserPreferenceKeys.hasNamedDevice, false),
        preferredTheme = enumValue(
            UserPreferenceKeys.preferredTheme,
            ThemePreference.SYSTEM,
        ),
        preferredContentAppearance = enumValue(
            UserPreferenceKeys.preferredContentAppearance,
            ContentAppearance.LIST,
        ),
        preferredCardImageSizing = enumValue(
            UserPreferenceKeys.preferredCardImageSizing,
            CardImageSizing.SMALL,
        ),
        preferredCollectionSorting = enumValue(
            UserPreferenceKeys.preferredCollectionSorting,
            SortType.CREATED_AT,
        ),
        preferredBookmarkSorting = enumValue(
            UserPreferenceKeys.preferredBookmarkSorting,
            SortType.CREATED_AT,
        ),
        preferredSortOrder = enumValue(
            UserPreferenceKeys.preferredSortOrder,
            SortOrderType.ASCENDING,
        ),
        preferredFabPosition = enumValue(
            UserPreferenceKeys.preferredFabPosition,
            FabPosition.CENTER,
        ),
        disableBackgroundAnimation = getBoolean(
            UserPreferenceKeys.disableBackgroundAnimation,
            false
        ),
        deviceId = this[UserPreferenceKeys.deviceId].orEmpty(),
        deviceName = this[UserPreferenceKeys.deviceName].orEmpty(),
        showRecents = getBoolean(UserPreferenceKeys.showRecents, true),
        showMenuBarItem = getBoolean(UserPreferenceKeys.showMenuBarItem, true),
        useSimplifiedShare = getBoolean(UserPreferenceKeys.useSimplifiedShare, false),
        preventDeletionSync = getBoolean(UserPreferenceKeys.preventDeletionSync, false),
        announcementsYaba1_2Update = getBoolean(UserPreferenceKeys.announcementsYaba12, true),
        announcementsYaba1_3Update = getBoolean(UserPreferenceKeys.announcementsYaba13, true),
        announcementsYaba1_4Update = getBoolean(UserPreferenceKeys.announcementsYaba14, true),
        announcementsYaba1_5Update = getBoolean(UserPreferenceKeys.announcementsYaba15, true),
        announcementsCloudKitDrop = getBoolean(UserPreferenceKeys.announcementsCloudKitDrop, true),
        announcementsCloudKitDropUrgent = getBoolean(
            UserPreferenceKeys.announcementsCloudKitDropUrgent,
            true,
        ),
        announcementsCloudKitDatabaseWipe = getBoolean(
            UserPreferenceKeys.announcementsCloudKitDatabaseWipe,
            true,
        ),
        announcementsLegalsUpdate = getBoolean(UserPreferenceKeys.announcementsLegalsUpdate, true),
        announcementsLegalsUpdate2 = getBoolean(
            UserPreferenceKeys.announcementsLegalsUpdate2,
            true
        ),
        migrationCompleted = getBoolean(UserPreferenceKeys.migrationCompleted, false),
    )

private fun Preferences.getBoolean(
    key: Preferences.Key<Boolean>,
    default: Boolean,
): Boolean = this[key] ?: default

private inline fun <reified T : Enum<T>> Preferences.enumValue(
    key: Preferences.Key<String>,
    default: T,
): T = this[key]?.let { value ->
    runCatching { enumValueOf<T>(value) }.getOrNull()
} ?: default

internal expect fun resolveUserSettingsDataStoreFile(
    subdir: String,
    fileName: String,
): PlatformFile
