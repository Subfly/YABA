package dev.subfly.yaba.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for readable content assets (images).
 *
 * Indexes immutable assets stored at
 * `/bookmarks/<id>/assets/<assetId>.<ext>`.
 *
 * This is a derived cache - the filesystem is authoritative.
 */
@Entity(
    tableName = "readable_assets",
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
    ],
)
data class ReadableAssetEntity(
    @PrimaryKey val id: String,
    val bookmarkId: String,
    val relativePath: String,
)
