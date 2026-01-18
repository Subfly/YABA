@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.common

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Canonical ID type for all entities in the Core.
 * Using a typealias to String for clarity and easy migration.
 */
typealias EntityId = String

/**
 * Single source for generating new entity IDs.
 *
 * This is the only place in Core that should use [kotlin.uuid.Uuid] directly.
 * All other code should work with String IDs.
 */
object IdGenerator {
    /**
     * Generates a new unique entity ID as a String.
     */
    fun newId(): EntityId = Uuid.random().toString()
}
