package dev.subfly.yabacore.filesystem.json

import kotlinx.serialization.Serializable

/**
 * JSON schema for folder metadata stored at `/folders/<uuid>/meta.json`.
 *
 * This is the authoritative representation of a folder in the filesystem.
 * The SQLite database is a derived cache that can be rebuilt from these files.
 */
@Serializable
data class FolderMetaJson(
    val id: String,
    val parentId: String?,
    val label: String,
    val description: String?,
    val icon: String,
    val colorCode: Int,
    val order: Int,
    val createdAt: Long,
    val editedAt: Long,
    /** Vector clock: Map of deviceId to sequence number */
    val clock: Map<String, Long>,
    /** True if this is a hidden system folder. */
    val isHidden: Boolean = false,
)
