package dev.subfly.yabacore.database.domain

import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Instant

internal data class TagDomainModel(
    val id: String,
    val label: String,
    val icon: String,
    val color: YabaColor,
    val createdAt: Instant,
    val editedAt: Instant,
    val order: Int,
)
