package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "bookmarks",
    indices =
        [
            Index(value = ["folderId"]),
            Index(value = ["editedAt"]),
            Index(value = ["title"]),
        ]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val url: String,
    val iconName: String,
    val color: Int,
    val folderId: String,
    val createdAt: Instant,
    val editedAt: Instant,
    val version: Int,
)
