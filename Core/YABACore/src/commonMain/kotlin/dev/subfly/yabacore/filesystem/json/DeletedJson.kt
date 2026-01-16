package dev.subfly.yabacore.filesystem.json

import kotlinx.serialization.Serializable

/**
 * JSON schema for deletion tombstone stored at:
 * - `/folders/<uuid>/deleted.json`
 * - `/tags/<uuid>/deleted.json`
 * - `/bookmarks/<uuid>/deleted.json`
 *
 * When a `deleted.json` file exists in an entity's folder, that entity is
 * considered permanently deleted (a "death certificate").
 *
 * Rules:
 * - If `deleted.json` exists, the object is dead and never resurrects
 * - Tombstones are kept forever
 * - All other files in the entity folder should be removed when deleted
 * - The entity folder itself is preserved to hold the tombstone
 */
@Serializable
data class DeletedJson(
    val id: String,
    val deleted: Boolean = true,
    /** Vector clock at time of deletion: Map of deviceId to sequence number */
    val clock: Map<String, Long>,
)
