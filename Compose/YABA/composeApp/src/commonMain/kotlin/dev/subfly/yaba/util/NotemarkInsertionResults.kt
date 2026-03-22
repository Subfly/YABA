package dev.subfly.yaba.util

import kotlinx.serialization.Serializable

@Serializable
data class NotemarkTableSheetResult(
    val rows: Int,
    val cols: Int,
    val withHeaderRow: Boolean = false,
)
