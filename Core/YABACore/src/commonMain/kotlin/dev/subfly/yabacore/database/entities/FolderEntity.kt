package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.utils.YabaColor

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["label"]),
        Index(value = ["editedAt"]),
        Index(value = ["isHidden"]),
    ],
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val label: String,
    val description: String?,
    val icon: String,
    val color: YabaColor,
    val createdAt: Long,
    val editedAt: Long,
    /** True if this is a hidden system folder. Hidden folders are filtered from UI queries. */
    val isHidden: Boolean = false,
)
