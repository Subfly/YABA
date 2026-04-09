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

@Serializable
enum class InlineSheetAction {
    INSERT_OR_UPDATE,
    REMOVE,
}

@Serializable
data class InlineLinkSheetResult(
    val text: String,
    val url: String,
    val action: InlineSheetAction = InlineSheetAction.INSERT_OR_UPDATE,
    val editPos: Int? = null,
    /** When set, targets an Excalidraw element on the canvas (instead of [editPos] in the note doc). */
    val canvasElementId: String? = null,
)

@Serializable
data class InlineMentionSheetResult(
    val text: String,
    val bookmarkId: String,
    val bookmarkKindCode: Int,
    val bookmarkLabel: String,
    val action: InlineSheetAction = InlineSheetAction.INSERT_OR_UPDATE,
    val editPos: Int? = null,
    /** When set, targets an Excalidraw element on the canvas (instead of [editPos] in the note doc). */
    val canvasElementId: String? = null,
)

@Serializable
enum class InlineActionChoice {
    EDIT,
    OPEN,
    REMOVE,
}
