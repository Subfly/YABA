package dev.subfly.yabacore.database.models

import androidx.room.ColumnInfo

data class TagWithCount(
    val id: String,
    val label: String,
    val iconName: String,
    val color: Int,
    val version: Int,
    @ColumnInfo(name = "bookmarkCount") val bookmarkCount: Long,
)
