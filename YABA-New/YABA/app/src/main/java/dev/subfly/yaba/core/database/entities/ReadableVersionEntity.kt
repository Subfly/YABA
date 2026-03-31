package dev.subfly.yaba.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for readable content versions.
 *
 * Indexes immutable readable content stored at
 * `/bookmarks/<id>/readable/<versionId>.json` (rich-text document JSON).
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
        Index(value = ["createdAt"]),
    ],
)
data class ReadableVersionEntity(
    @PrimaryKey val id: String,
    val bookmarkId: String,
    val createdAt: Long,
    val relativePath: String,
)
