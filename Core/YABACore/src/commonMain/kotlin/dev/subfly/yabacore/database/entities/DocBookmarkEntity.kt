package dev.subfly.yabacore.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "doc_bookmarks",
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
data class DocBookmarkEntity(
    @PrimaryKey val bookmarkId: String,
    val summary: String? = null,
)
