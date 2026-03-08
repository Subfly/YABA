package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "image_bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookmarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookmarkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookmarkId"], unique = true),
    ],
)
data class ImageBookmarkEntity(
    @PrimaryKey val bookmarkId: String,
    val summary: String? = null,
)
