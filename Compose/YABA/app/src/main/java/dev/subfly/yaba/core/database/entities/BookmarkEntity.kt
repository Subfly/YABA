package dev.subfly.yaba.core.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.subfly.yaba.core.model.utils.BookmarkKind

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["kind"]),
        Index(value = ["label"]),
        Index(value = ["editedAt"]),
    ],
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val folderId: String,
    val kind: BookmarkKind,
    val label: String,
    val description: String? = null,
    val createdAt: Long,
    val editedAt: Long,
    val viewCount: Long = 0,
    val isPinned: Boolean = false,
    val localImagePath: String? = null,
    val localIconPath: String? = null,
)
