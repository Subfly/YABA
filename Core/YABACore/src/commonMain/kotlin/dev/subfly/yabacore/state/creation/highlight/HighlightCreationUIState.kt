package dev.subfly.yabacore.state.creation.highlight

import dev.subfly.yabacore.model.highlight.ReadableSelectionDraft
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.utils.YabaColor

data class HighlightCreationUIState(
    /** Selection draft when creating from reader. Null when editing. */
    val selectionDraft: ReadableSelectionDraft? = null,
    /** Existing highlight when editing. Null when creating. */
    val highlight: HighlightUiModel? = null,
    /** BookmarkId when editing (from OnInitWithHighlight). */
    val bookmarkIdForEdit: String? = null,
    val selectedColor: YabaColor = YabaColor.YELLOW,
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = highlight != null
    val quoteText: String
        get() = selectionDraft?.quote?.displayText
            ?: highlight?.quoteText
            ?: ""

    val bookmarkId: String?
        get() = selectionDraft?.bookmarkId ?: bookmarkIdForEdit

    /** Enough to save: editing, or new PDF/rich selection with quote (or PDF anchor). */
    val hasValidSelection: Boolean
        get() = when {
            highlight != null -> true
            selectionDraft != null ->
                selectionDraft.quote.displayText.isNotBlank() ||
                    selectionDraft.pdfAnchor != null
            else -> false
        }
}
