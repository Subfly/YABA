package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["editedAt"]),
        Index(value = ["order"]),
    ]
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val label: String,
    val iconName: String,
    val color: Int,
    val parentId: String?,
    val order: Int,
    val createdAt: Instant,
    val editedAt: Instant,
    val version: Int,
)


