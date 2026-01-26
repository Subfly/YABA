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
    @PrimaryKey val id: String,
    val bookmarkId: String,
    val contentVersion: Int,
    val createdAt: Long,
    val relativePath: String,
    val title: String?,
    val author: String?,
)
