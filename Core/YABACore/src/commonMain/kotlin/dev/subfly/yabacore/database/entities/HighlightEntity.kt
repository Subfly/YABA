package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.utils.YabaColor

/**
 * Room entity for highlight annotations.
 *
 * Highlights are stored in DB only. Section-anchored:
 * startSectionKey, startOffsetInSection, endSectionKey, endOffsetInSection.
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
        ForeignKey(
            entity = ReadableVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["readableVersionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookmarkId"]),
        Index(value = ["bookmarkId", "readableVersionId"]),
        Index(value = ["editedAt"]),
    ],
)
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookmarkId: String,
    val readableVersionId: String,
    val startSectionKey: String,
    val startOffsetInSection: Int,
    val endSectionKey: String,
    val endOffsetInSection: Int,
    val colorRole: YabaColor,
    val note: String?,
    val createdAt: Long,
    val editedAt: Long,
)
