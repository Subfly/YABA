package dev.subfly.yaba.core.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.subfly.yaba.core.model.utils.DocmarkType

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
    @ColumnInfo(defaultValue = "PDF")
    val type: DocmarkType = DocmarkType.PDF,
    val metadataTitle: String? = null,
    val metadataDescription: String? = null,
    val metadataAuthor: String? = null,
    val metadataDate: String? = null,
)
