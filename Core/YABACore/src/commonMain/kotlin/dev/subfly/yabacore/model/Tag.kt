package dev.subfly.yabacore.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
data class Tag(
        val id: Uuid,
        val label: String,
        val icon: String,
        val color: YabaColor,
        val createdAt: Instant,
        val editedAt: Instant,
        val order: Int,
)
