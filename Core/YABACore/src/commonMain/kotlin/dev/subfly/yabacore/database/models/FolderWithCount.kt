package dev.subfly.yabacore.database.models

import androidx.room.ColumnInfo

data class FolderWithCount(
    val id: String,
    val label: String,
    val iconName: String,
    val color: Int,
    val parentId: String?,
    val order: Int,
    val version: Int,
    @ColumnInfo(name = "bookmarkCount") val bookmarkCount: Long,
)
