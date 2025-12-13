package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.utils.YabaColor
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["order"]),
        Index(value = ["label"]),
        Index(value = ["editedAt"]),
    ],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val label: String,
    val icon: String,
    val color: YabaColor,
    val order: Int,
    val createdAt: Long,
    val editedAt: Long,
)
