package dev.subfly.yabacore.state.creation.highlight

import dev.subfly.yabacore.model.highlight.ReadableSelectionDraft
import dev.subfly.yabacore.model.utils.YabaColor

sealed class HighlightCreationEvent {
    /** Initialize for creating a new highlight from reader selection. */
    data class OnInitWithSelection(val draft: ReadableSelectionDraft) : HighlightCreationEvent()

    /** Initialize for editing an existing highlight. Requires bookmarkId for loading. */
    data class OnInitWithHighlight(
        val bookmarkId: String,
        val highlightId: String,
    ) : HighlightCreationEvent()

    data class OnSelectNewColor(val newColor: YabaColor) : HighlightCreationEvent()
    data class OnChangeNote(val note: String) : HighlightCreationEvent()
    data class OnSave(
        val onSavedCallback: () -> Unit,
        val onErrorCallback: (Throwable) -> Unit,
    ) : HighlightCreationEvent()

    data class OnDelete(
        val onDeletedCallback: () -> Unit,
        val onErrorCallback: (Throwable) -> Unit,
    ) : HighlightCreationEvent()
}
