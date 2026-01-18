package dev.subfly.yabacore

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.queue.CoreOperationQueue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.Volatile

/**
 * Unified initialization for YABACore.
 *
 * Call [initialize] once at app startup to set up all core systems:
 * - SQLite databases (main + events)
 * - DeviceIdProvider with stable deviceId from preferences
 * - CoreOperationQueue for sequential persistence operations
 *
 * This ensures consistent initialization across all platforms and prevents
 * issues like unstable deviceIds breaking vector clocks.
 *
 * Usage:
 * ```kotlin
 * // Android (in Application or Activity onCreate)
 * CoreRuntime.initialize(platformContext = context)
 *
 * // Desktop/JVM
 * CoreRuntime.initialize()
 *
 * // iOS (called from Swift)
 * CoreRuntime.initialize()
 * ```
 */
object CoreRuntime {
    @Volatile
    private var initialized: Boolean = false

    /**
     * Initializes all YABACore systems.
     *
     * This method is idempotent - calling it multiple times has no effect after
     * the first successful initialization.
     *
     * @param platformContext Platform-specific context (Android Context, or null for other platforms)
     */
    fun initialize(platformContext: Any? = null) {
        if (initialized) return

        // 1. Initialize databases
        DatabaseProvider.initialize(platformContext)
        EventsDatabaseProvider.initialize(platformContext)

        // 2. Initialize DeviceIdProvider with persisted deviceId
        // UserPreferencesStore.ensureDefaults() is called during lazy init,
        // which generates a deviceId if not already present
        val preferences = runBlocking {
            SettingsStores.userPreferences.preferencesFlow.first()
        }
        DeviceIdProvider.initialize(preferences.deviceId)

        // 3. Start the operation queue (app-lifetime worker)
        CoreOperationQueue.start()

        initialized = true
    }

    /**
     * Returns whether CoreRuntime has been initialized.
     */
    fun isInitialized(): Boolean = initialized

    /**
     * Resets the initialization state. For testing purposes only.
     */
    internal fun reset() {
        initialized = false
        DeviceIdProvider.reset()
    }
}
