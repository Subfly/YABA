package dev.subfly.yabacore.database.models

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import dev.subfly.yabacore.database.entities.TagEntity

data class TagWithBookmarkCount(
    @Embedded val tag: TagEntity,
    @ColumnInfo(name = "bookmarkCount") val bookmarkCount: Long,
)

