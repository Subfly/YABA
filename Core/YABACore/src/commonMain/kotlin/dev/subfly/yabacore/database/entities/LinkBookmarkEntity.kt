package dev.subfly.yabacore.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "link_bookmarks",
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
data class LinkBookmarkEntity(
    @PrimaryKey val bookmarkId: String,
    val url: String,
    val domain: String,
    val videoUrl: String?,
)
