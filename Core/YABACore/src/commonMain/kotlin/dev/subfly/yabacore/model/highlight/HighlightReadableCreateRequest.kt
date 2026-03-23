package dev.subfly.yabacore.model.highlight

import dev.subfly.yabacore.model.utils.YabaColor

/**
 * Result payload for creating a READABLE/NOTE highlight after the user confirms the sheet.
 * The owning screen applies the TipTap mark, persists JSON, then calls [dev.subfly.yabacore.managers.HighlightManager].
 */
data class HighlightReadableCreateRequest(
    val selectionDraft: ReadableSelectionDraft,
    val colorRole: YabaColor,
    val note: String?,
)
