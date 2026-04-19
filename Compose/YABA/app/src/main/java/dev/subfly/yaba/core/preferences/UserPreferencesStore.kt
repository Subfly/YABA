package dev.subfly.yaba.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.FabPosition
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType
import dev.subfly.yaba.core.model.utils.ThemePreference
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SETTINGS_SUBDIR = "settings"
private const val ENCRYPTED_SETTINGS_FILE = "user_settings.encrypted"

class UserPreferencesStore
internal constructor(
    private val dataStore: DataStore<UserPreferences>,
) {
    internal val dataStoreHandle: DataStore<UserPreferences> = dataStore

    val preferencesFlow: Flow<UserPreferences> = dataStore.data

    suspend fun get(): UserPreferences = dataStore.data.first()

    suspend fun setHasPassedOnboarding(value: Boolean) =
        update { it.copy(hasPassedOnboarding = value) }

    suspend fun setHasNamedDevice(value: Boolean) = update { it.copy(hasNamedDevice = value) }

    suspend fun setPreferredTheme(ordinal: Int) =
        setPreferredTheme(enumFromOrdinal(ordinal, ThemePreference.SYSTEM))

    suspend fun setPreferredTheme(value: ThemePreference) =
        update { it.copy(preferredTheme = value) }

    suspend fun setPreferredBookmarkAppearance(ordinal: Int) =
        setPreferredBookmarkAppearance(enumFromOrdinal(ordinal, BookmarkAppearance.LIST))

    suspend fun setPreferredBookmarkAppearance(value: BookmarkAppearance) =
        update { it.copy(preferredBookmarkAppearance = value) }

    suspend fun setPreferredCardImageSizing(ordinal: Int) =
        setPreferredCardImageSizing(enumFromOrdinal(ordinal, CardImageSizing.SMALL))

    suspend fun setPreferredCardImageSizing(value: CardImageSizing) =
        update { it.copy(preferredCardImageSizing = value) }

    suspend fun setPreferredCollectionSorting(ordinal: Int) =
        setPreferredCollectionSorting(enumFromOrdinal(ordinal, SortType.CREATED_AT))

    suspend fun setPreferredCollectionSorting(value: SortType) =
        update { it.copy(preferredCollectionSorting = value) }

    suspend fun setPreferredBookmarkSorting(ordinal: Int) =
        setPreferredBookmarkSorting(enumFromOrdinal(ordinal, SortType.CREATED_AT))

    suspend fun setPreferredBookmarkSorting(value: SortType) =
        update { it.copy(preferredBookmarkSorting = value) }

    suspend fun setPreferredCollectionSortOrder(ordinal: Int) =
        setPreferredCollectionSortOrder(enumFromOrdinal(ordinal, SortOrderType.ASCENDING))

    suspend fun setPreferredCollectionSortOrder(value: SortOrderType) =
        update { it.copy(preferredCollectionSortOrder = value) }

    suspend fun setPreferredBookmarkSortOrder(ordinal: Int) =
        setPreferredBookmarkSortOrder(enumFromOrdinal(ordinal, SortOrderType.DESCENDING))

    suspend fun setPreferredBookmarkSortOrder(value: SortOrderType) =
        update { it.copy(preferredBookmarkSortOrder = value) }

    suspend fun setPreferredFabPosition(ordinal: Int) =
        setPreferredFabPosition(enumFromOrdinal(ordinal, FabPosition.CENTER))

    suspend fun setPreferredFabPosition(value: FabPosition) =
        update { it.copy(preferredFabPosition = value) }

    suspend fun setDisableBackgroundAnimation(value: Boolean) =
        update { it.copy(disableBackgroundAnimation = value) }

    suspend fun setDeviceId(value: String) = update { it.copy(deviceId = value) }

    suspend fun setDeviceName(value: String) = update { it.copy(deviceName = value) }

    suspend fun setShowRecents(value: Boolean) = update { it.copy(showRecents = value) }

    suspend fun setShowMenuBarItem(value: Boolean) = update { it.copy(showMenuBarItem = value) }

    suspend fun setUseSimplifiedShare(value: Boolean) =
        update { it.copy(useSimplifiedShare = value) }

    suspend fun setPreventDeletionSync(value: Boolean) =
        update { it.copy(preventDeletionSync = value) }

    suspend fun setAnnouncementsYaba12(value: Boolean) =
        update { it.copy(announcementsYaba1_2Update = value) }

    suspend fun setAnnouncementsYaba13(value: Boolean) =
        update { it.copy(announcementsYaba1_3Update = value) }

    suspend fun setAnnouncementsYaba14(value: Boolean) =
        update { it.copy(announcementsYaba1_4Update = value) }

    suspend fun setAnnouncementsYaba15(value: Boolean) =
        update { it.copy(announcementsYaba1_5Update = value) }

    suspend fun setAnnouncementsCloudKitDrop(value: Boolean) =
        update { it.copy(announcementsCloudKitDrop = value) }

    suspend fun setAnnouncementsCloudKitDropUrgent(value: Boolean) =
        update { it.copy(announcementsCloudKitDropUrgent = value) }

    suspend fun setAnnouncementsCloudKitDatabaseWipe(value: Boolean) =
        update { it.copy(announcementsCloudKitDatabaseWipe = value) }

    suspend fun setAnnouncementsLegalsUpdate(value: Boolean) =
        update { it.copy(announcementsLegalsUpdate = value) }

    suspend fun setAnnouncementsLegalsUpdate2(value: Boolean) =
        update { it.copy(announcementsLegalsUpdate2 = value) }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun ensureDefaults() {
        dataStore.updateData { prefs ->
            if (prefs.deviceId.isBlank()) prefs.copy(
                deviceId = Uuid.generateV4().toString()
            )
            else prefs
        }
    }

    private suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.updateData(transform)
    }
}

object UserPreferencesStoreFactory {
    fun create(
        context: Context,
        coroutineContext: CoroutineContext = Dispatchers.Default,
    ): UserPreferencesStore {
        val dataStore =
            DataStoreFactory.create(
                serializer = UserPreferencesSerializer,
                scope = CoroutineScope(SupervisorJob() + coroutineContext),
                produceFile = { resolveEncryptedUserSettingsFile(context) },
            )
        val store = UserPreferencesStore(dataStore)
        runBlocking {
            store.ensureDefaults()
        }
        return store
    }
}

object SettingsStores {
    @Volatile
    private var appContext: Context? = null

    /** Call once from [android.app.Activity.onCreate] before accessing [userPreferences]. */
    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
    }

    val userPreferences: UserPreferencesStore by lazy {
        val ctx = appContext
            ?: error("SettingsStores.initialize(context) must be called before accessing userPreferences")
        UserPreferencesStoreFactory.create(ctx)
    }
}

private fun resolveEncryptedUserSettingsFile(context: Context): File {
    val dir = File(context.filesDir, SETTINGS_SUBDIR).apply { mkdirs() }
    return File(dir, ENCRYPTED_SETTINGS_FILE)
}
