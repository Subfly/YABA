package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.YabaColor
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["order"]),
        Index(value = ["label"]),
        Index(value = ["editedAt"]),
    ],
)
data class FolderEntity(
    @PrimaryKey val id: Uuid,
    val parentId: Uuid?,
    val label: String,
    val description: String?,
    val icon: String,
    val color: YabaColor,
    val order: Int,
    val createdAt: Instant,
    val editedAt: Instant,
)
