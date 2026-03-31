package dev.subfly.yaba.core.database.models

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import dev.subfly.yaba.core.database.entities.FolderEntity

data class FolderWithBookmarkCount(
    @Embedded val folder: FolderEntity,
    @ColumnInfo(name = "bookmarkCount") val bookmarkCount: Long,
)

