@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.preload.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class PreloadData(
    val folders: List<PreloadCollection>,
    val tags: List<PreloadCollection>,
)

@Serializable
data class PreloadCollection(
    val id: String,
    val label: String,
    val icon: String,
    val color: Int,
)
