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
    /** Highlight UUID */
    @PrimaryKey val id: String,
    /** Parent bookmark ID */
    val bookmarkId: String,
    /** Content version this highlight references */
    val contentVersion: Int,
    /** Start anchor block ID */
    val startBlockId: String,
    /** Start anchor inline path (comma-separated integers) */
    val startInlinePath: String,
    /** Start anchor character offset */
    val startOffset: Int,
    /** End anchor block ID */
    val endBlockId: String,
    /** End anchor inline path (comma-separated integers) */
    val endInlinePath: String,
    /** End anchor character offset */
    val endOffset: Int,
    /** Highlight color role */
    val colorRole: YabaColor,
    /** Optional user note */
    val note: String?,
    /** Relative path from YABA root to the JSON file */
    val relativePath: String,
    /** Creation timestamp in epoch milliseconds */
    val createdAt: Long,
    /** Last edit timestamp in epoch milliseconds */
    val editedAt: Long,
)
