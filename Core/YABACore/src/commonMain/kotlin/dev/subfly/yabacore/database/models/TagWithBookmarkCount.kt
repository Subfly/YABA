package dev.subfly.yabacore.database.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import dev.subfly.yabacore.database.entities.TagEntity

data class TagWithBookmarkCount(
    @Embedded val tag: TagEntity,
    @ColumnInfo(name = "bookmarkCount") val bookmarkCount: Long,
)

