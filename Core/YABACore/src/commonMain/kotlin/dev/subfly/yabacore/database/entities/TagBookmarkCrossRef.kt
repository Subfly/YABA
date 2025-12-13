package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
@Entity(
    tableName = "tag_bookmarks",
    primaryKeys = ["tagId", "bookmarkId"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BookmarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookmarkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookmarkId"]),
        Index(value = ["tagId"]),
    ],
)
data class TagBookmarkCrossRef(
    val tagId: String,
    val bookmarkId: String,
)

