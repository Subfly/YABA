@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package dev.subfly.yabacore.database

import kotlin.concurrent.Volatile
import kotlin.uuid.Uuid

/**
 * Provides a stable device identifier used by sync and vector clocks.
 *
 * IMPORTANT: Call [initialize] once at app startup (after UserPreferencesStore is ready)
 * to configure the provider with the persisted deviceId. If not initialized, a process-stable
 * fallback UUID will be used (memoized on first access).
 */
object DeviceIdProvider {
    @Volatile
    private var cachedDeviceId: String? = null

    @Volatile
    private var initialized: Boolean = false

    /**
     * Initializes the DeviceIdProvider with a stable deviceId.
     *
     * This should be called once at app startup with the persisted deviceId from UserPreferences.
     * Once initialized, [get] will always return this deviceId.
     *
     * @param deviceId The stable device identifier (typically from UserPreferencesStore)
     */
    fun initialize(deviceId: String) {
        if (deviceId.isNotBlank()) {
            cachedDeviceId = deviceId
            initialized = true
        }
    }

    /**
     * Returns the stable device identifier.
     *
     * If [initialize] was called, returns the configured deviceId.
     * Otherwise, returns a process-stable fallback UUID (memoized on first call).
     */
    fun get(): String {
        cachedDeviceId?.let { return it }
        return Uuid.random().toString()
    }

    /**
     * Returns whether the provider has been explicitly initialized.
     */
    fun isInitialized(): Boolean = initialized

    /**
     * Clears the cached deviceId. For testing purposes only.
     */
    internal fun reset() {
        cachedDeviceId = null
        initialized = false
    }
}
