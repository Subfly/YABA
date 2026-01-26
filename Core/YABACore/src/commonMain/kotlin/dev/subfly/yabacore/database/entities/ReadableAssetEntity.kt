package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.utils.ReadableAssetRole

/**
 * Room entity for readable content assets (images).
 *
 * Indexes immutable assets stored at
 * `/bookmarks/<id>/content/assets/<assetId>.<ext>`.
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
    val role: ReadableAssetRole,
    val relativePath: String,
)
