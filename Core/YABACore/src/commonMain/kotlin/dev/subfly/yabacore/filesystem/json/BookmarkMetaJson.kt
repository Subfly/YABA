package dev.subfly.yabacore.filesystem.json

import kotlinx.serialization.Serializable

/**
 * JSON schema for base bookmark metadata stored at `/bookmarks/<uuid>/meta.json`.
 *
 * This contains common bookmark fields shared across all bookmark types.
 * Subtype-specific data (e.g., link URL/domain) is stored in separate files
 * like `link.json`.
 *
 * This is the authoritative representation of a bookmark in the filesystem.
 * The SQLite database is a derived cache that can be rebuilt from these files.
 */
@Serializable
data class BookmarkMetaJson(
    val id: String,
    val folderId: String,
    /** Bookmark kind code (see BookmarkKind enum) */
    val kind: Int,
    val label: String,
    val description: String?,
    val createdAt: Long,
    val editedAt: Long,
    val viewCount: Long,
    val isPrivate: Boolean,
    val isPinned: Boolean,
    /** Relative path to preview image within the bookmark's content directory */
    val localImagePath: String?,
    /** Relative path to preview icon within the bookmark's content directory */
    val localIconPath: String?,
    /** List of tag IDs associated with this bookmark */
    val tagIds: List<String>,
    /** Vector clock: Map of deviceId to sequence number */
    val clock: Map<String, Long>,
)
