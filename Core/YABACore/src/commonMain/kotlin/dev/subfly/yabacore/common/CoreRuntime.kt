package dev.subfly.yabacore.common

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.notifications.initializePlatformNotifications
import dev.subfly.yabacore.queue.CoreOperationQueue
import kotlin.concurrent.Volatile

/**
 * Unified initialization for YABACore.
 *
 * Call [initialize] once at app startup to set up all core systems:
 * - SQLite database (main)
 * - CoreOperationQueue for sequential persistence operations
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

        // 1. Initialize database
        DatabaseProvider.initialize(platformContext)

        // 2. Initialize platform notification support (Android stores the Context)
        initializePlatformNotifications(platformContext)

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
    }
}