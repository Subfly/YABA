package dev.subfly.yaba.core.state.creation.annotation

import dev.subfly.yaba.core.model.annotation.ReadableSelectionDraft
import dev.subfly.yaba.core.model.utils.YabaColor

sealed class AnnotationCreationEvent {
    /** Initialize for creating a new annotation from reader selection. */
    data class OnInitWithSelection(val draft: ReadableSelectionDraft) : AnnotationCreationEvent()

    /** Initialize for editing an existing annotation. Requires bookmarkId for loading. */
    data class OnInitWithAnnotation(
        val bookmarkId: String,
        val annotationId: String,
    ) : AnnotationCreationEvent()

    data class OnSelectNewColor(val newColor: YabaColor) : AnnotationCreationEvent()
    data class OnChangeNote(val note: String) : AnnotationCreationEvent()
    data class OnSave(
        val onSavedCallback: () -> Unit,
        val onErrorCallback: (Throwable) -> Unit,
    ) : AnnotationCreationEvent()

    data class OnDelete(
        val onDeletedCallback: () -> Unit,
        val onErrorCallback: (Throwable) -> Unit,
    ) : AnnotationCreationEvent()
}
