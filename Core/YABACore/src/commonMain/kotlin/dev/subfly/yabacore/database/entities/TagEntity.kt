package dev.subfly.yabacore.database.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.subfly.yabacore.model.utils.YabaColor

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["label"]),
        Index(value = ["editedAt"]),
        Index(value = ["isHidden"]),
    ],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val label: String,
    val icon: String,
    val color: YabaColor,
    val createdAt: Long,
    val editedAt: Long,
    /** True if this is a hidden system tag. Hidden tags are filtered from UI queries. */
    val isHidden: Boolean = false,
)
