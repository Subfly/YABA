package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.utils.BookmarkKind
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Entity(
    tableName = "bookmarks",
    foreignKeys =
        [
            ForeignKey(
                entity = FolderEntity::class,
                parentColumns = ["id"],
                childColumns = ["folderId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices =
        [
            Index(value = ["folderId"]),
            Index(value = ["kind"]),
            Index(value = ["label"]),
            Index(value = ["editedAt"]),
        ],
)
data class BookmarkEntity(
    @PrimaryKey val id: Uuid,
    val folderId: Uuid,
    val kind: BookmarkKind,
    val label: String,
    val createdAt: Instant,
    val editedAt: Instant,
)
