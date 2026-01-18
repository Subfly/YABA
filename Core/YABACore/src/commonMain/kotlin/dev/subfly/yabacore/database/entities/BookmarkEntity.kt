package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.utils.BookmarkKind
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
    val isPrivate: Boolean = false,
    val isPinned: Boolean = false,
    val localImagePath: String? = null,
    val localIconPath: String? = null,
)
