package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["order"]),
        Index(value = ["label"]),
        Index(value = ["editedAt"]),
    ],
)
data class TagEntity(
    @PrimaryKey val id: Uuid,
    val label: String,
    val icon: String,
    val color: YabaColor,
    val order: Int,
    val createdAt: Instant,
    val editedAt: Instant,
)
