package dev.subfly.yabacore.preferences

/**
 * Centralized hook to migrate platform-specific settings (e.g. Darwin AppStorage / UserDefaults)
 * into the shared DataStore-backed [UserPreferencesStore].
 */
object PreferencesMigration {
    suspend fun migrateIfNeeded(store: UserPreferencesStore = SettingsStores.userPreferences) {
        migratePlatformPreferencesIfNeeded(store)
    }
}

internal expect suspend fun migratePlatformPreferencesIfNeeded(store: UserPreferencesStore)

