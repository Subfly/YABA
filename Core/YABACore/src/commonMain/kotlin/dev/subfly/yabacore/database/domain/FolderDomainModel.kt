package dev.subfly.yabacore.database.domain

import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Instant

internal data class FolderDomainModel(
    val id: String,
    val parentId: String?,
    val label: String,
    val description: String?,
    val icon: String,
    val color: YabaColor,
    val createdAt: Instant,
    val editedAt: Instant,
    val order: Int,
)
