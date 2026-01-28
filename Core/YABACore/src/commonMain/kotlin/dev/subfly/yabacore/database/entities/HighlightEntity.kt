package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.utils.YabaColor

/**
 * Room entity for highlight annotations.
 *
 * Indexes mutable highlight data stored at
 * `/bookmarks/<id>/content/annotations/<highlightId>.json`.
 *
 * This is a derived cache - the filesystem is authoritative.
 * Highlights are CRDT-merged for conflict-free sync.
 */
@Entity(
    tableName = "highlights",
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
        Index(value = ["bookmarkId", "contentVersion"]),
        Index(value = ["editedAt"]),
    ],
)
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookmarkId: String,
    val contentVersion: Int,
    val startOffset: Int,
    val endOffset: Int,
    val colorRole: YabaColor,
    val note: String?,
    val relativePath: String,
    val createdAt: Long,
    val editedAt: Long,
)
