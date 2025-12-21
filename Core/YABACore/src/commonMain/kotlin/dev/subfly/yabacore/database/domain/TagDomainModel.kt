package dev.subfly.yabacore.database.domain

import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal data class TagDomainModel(
    val id: Uuid,
    val label: String,
    val icon: String,
    val color: YabaColor,
    val createdAt: Instant,
    val editedAt: Instant,
    val order: Int,
)

