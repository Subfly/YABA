package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "bookmark_tag_cross_ref",
    primaryKeys = ["bookmarkId", "tagId"],
    indices = [
        Index(value = ["tagId"]),
        Index(value = ["bookmarkId"]),
    ]
)
data class BookmarkTagCrossRef(
    val bookmarkId: String,
    val tagId: String,
)


