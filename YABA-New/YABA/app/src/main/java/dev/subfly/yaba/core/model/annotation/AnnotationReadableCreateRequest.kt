package dev.subfly.yaba.core.model.annotation

import dev.subfly.yaba.core.model.utils.YabaColor

/**
 * Result payload for creating a READABLE annotation after the user confirms the sheet.
 * The owning screen applies the TipTap mark, persists JSON, then calls [dev.subfly.yabacore.managers.AnnotationManager].
 */
data class AnnotationReadableCreateRequest(
    val selectionDraft: ReadableSelectionDraft,
    val colorRole: YabaColor,
    val note: String?,
)
