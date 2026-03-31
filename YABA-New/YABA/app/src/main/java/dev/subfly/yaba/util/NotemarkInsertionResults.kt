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
enum class NotemarkInlineAction {
    INSERT_OR_UPDATE,
    REMOVE,
}

@Serializable
data class NotemarkLinkSheetResult(
    val text: String,
    val url: String,
    val action: NotemarkInlineAction = NotemarkInlineAction.INSERT_OR_UPDATE,
    val editPos: Int? = null,
)

@Serializable
data class NotemarkMentionSheetResult(
    val text: String,
    val bookmarkId: String,
    val bookmarkKindCode: Int,
    val bookmarkLabel: String,
    val action: NotemarkInlineAction = NotemarkInlineAction.INSERT_OR_UPDATE,
    val editPos: Int? = null,
)

@Serializable
enum class NotemarkInlineActionChoice {
    EDIT,
    OPEN,
    REMOVE,
}
