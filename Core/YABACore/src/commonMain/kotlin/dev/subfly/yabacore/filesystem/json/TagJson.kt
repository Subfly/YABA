package dev.subfly.yabacore.filesystem.json

import kotlinx.serialization.Serializable

/**
 * JSON schema for tag metadata stored at `/tags/<uuid>/meta.json`.
 *
 * This is the authoritative representation of a tag in the filesystem.
 * The SQLite database is a derived cache that can be rebuilt from these files.
 */
@Serializable
data class TagMetaJson(
    val id: String,
    val label: String,
    val icon: String,
    val colorCode: Int,
    val order: Int,
    val createdAt: Long,
    val editedAt: Long,
    /** Vector clock: Map of deviceId to sequence number */
    val clock: Map<String, Long>,
)
