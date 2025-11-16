package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.LinkType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "link_bookmarks",
    foreignKeys =
        [
            ForeignKey(
                entity = BookmarkEntity::class,
                parentColumns = ["id"],
                childColumns = ["bookmarkId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices =
        [
            Index(value = ["bookmarkId"], unique = true),
            Index(value = ["linkType"]),
        ],
)
data class LinkBookmarkEntity(
    @PrimaryKey val bookmarkId: Uuid,
    val description: String?,
    val url: String,
    val domain: String,
    val linkType: LinkType,
    val previewImageUrl: String?,
    val previewIconUrl: String?,
    val videoUrl: String?,
)
