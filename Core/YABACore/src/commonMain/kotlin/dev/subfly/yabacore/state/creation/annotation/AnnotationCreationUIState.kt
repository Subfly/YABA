package dev.subfly.yabacore.state.creation.annotation

import dev.subfly.yabacore.model.annotation.ReadableSelectionDraft
import dev.subfly.yabacore.model.ui.AnnotationUiModel
import dev.subfly.yabacore.model.utils.YabaColor

data class AnnotationCreationUIState(
    /** Selection draft when creating from reader. Null when editing. */
    val selectionDraft: ReadableSelectionDraft? = null,
    /** Existing annotation when editing. Null when creating. */
    val annotation: AnnotationUiModel? = null,
    /** BookmarkId when editing (from OnInitWithAnnotation). */
    val bookmarkIdForEdit: String? = null,
    val selectedColor: YabaColor = YabaColor.YELLOW,
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = annotation != null
    val quoteText: String
        get() = selectionDraft?.quote?.displayText
            ?: annotation?.quoteText
            ?: ""

    val bookmarkId: String?
        get() = selectionDraft?.bookmarkId ?: bookmarkIdForEdit

    /** Enough to save: editing, or new PDF/EPUB/rich selection with quote (or PDF/EPUB anchor). */
    val hasValidSelection: Boolean
        get() = when {
            annotation != null -> true
            selectionDraft != null ->
                selectionDraft.quote.displayText.isNotBlank() ||
                    selectionDraft.pdfAnchor != null ||
                    selectionDraft.epubAnchor != null
            else -> false
        }
}
