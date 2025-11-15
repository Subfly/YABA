package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "tags",
    indices =
        [
            Index(value = ["editedAt"]),
            Index(value = ["label"]),
        ]
)
data class TagEntity(
    @PrimaryKey val id: String,
    val label: String,
    val iconName: String,
    val color: Int,
    val createdAt: Instant,
    val editedAt: Instant,
    val version: Int,
)
