@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package dev.subfly.yabacore.database

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Simple holder for the device-id supplier used by sync. Set a custom provider once if you want a
 * stable, platform-specific ID; otherwise a random UUID will be used on first access.
 */
object DeviceIdProvider {
    private var provider: suspend () -> String = { Uuid.random().toString() }

    fun setProvider(block: suspend () -> String) {
        provider = block
    }

    suspend fun get(): String = provider()
}
