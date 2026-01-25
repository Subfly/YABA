package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for readable content versions.
 *
 * Indexes immutable readable document snapshots stored at
 * `/bookmarks/<id>/content/readable/vN.json`.
 *
 * This is a derived cache - the filesystem is authoritative.
 */
@Entity(
    tableName = "readable_versions",
    foreignKeys = [
        ForeignKey(
            entity = BookmarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookmarkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookmarkId"]),
        Index(value = ["bookmarkId", "contentVersion"], unique = true),
        Index(value = ["createdAt"]),
    ],
)
data class ReadableVersionEntity(
    /** Composite ID: <bookmarkId>_v<contentVersion> */
    @PrimaryKey val id: String,
    /** Parent bookmark ID */
    val bookmarkId: String,
    /** Content version number (1, 2, 3, ...) */
    val contentVersion: Int,
    /** Creation timestamp in epoch milliseconds */
    val createdAt: Long,
    /** Relative path from YABA root to the JSON file */
    val relativePath: String,
    /** Extracted document title (optional) */
    val title: String?,
    /** Extracted document author (optional) */
    val author: String?,
)
