package dev.subfly.yaba.core.database.models

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import dev.subfly.yaba.core.database.entities.TagEntity

data class TagWithBookmarkCount(
    @Embedded val tag: TagEntity,
    @ColumnInfo(name = "bookmarkCount") val bookmarkCount: Long,
)

