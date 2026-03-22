package dev.subfly.yaba.util

import kotlinx.serialization.Serializable

@Serializable
data class NotemarkTableSheetResult(
    val rows: Int,
    val cols: Int,
    val withHeaderRow: Boolean = false,
)

@Serializable
data class NotemarkMathSheetResult(
    val isBlock: Boolean,
    val latex: String,
    val isEdit: Boolean,
    val editPos: Int? = null,
)
