package dev.subfly.yaba.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
    /** Relative path under app bookmark root; full-res image for share/detail (not the compressed card preview). */
    val originalImageRelativePath: String? = null,
)
