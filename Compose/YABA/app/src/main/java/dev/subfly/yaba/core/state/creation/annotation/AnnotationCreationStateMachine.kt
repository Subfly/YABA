package dev.subfly.yaba.core.state.creation.annotation

import dev.subfly.yaba.core.common.IdGenerator
import dev.subfly.yaba.core.database.entities.AnnotationEntity
import dev.subfly.yaba.core.managers.AnnotationManager
import dev.subfly.yaba.core.model.annotation.AnnotationType
import dev.subfly.yaba.core.model.annotation.EpubAnnotationExtras
import dev.subfly.yaba.core.model.annotation.PdfAnnotationExtras
import dev.subfly.yaba.core.model.ui.AnnotationUiModel
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.state.base.BaseStateMachine
import kotlinx.serialization.json.Json

class AnnotationCreationStateMachine :
    BaseStateMachine<AnnotationCreationUIState, AnnotationCreationEvent>(
        initialState = AnnotationCreationUIState(),
    ) {
    private var isInitialized = false

    private val json = Json { ignoreUnknownKeys = true }

    override fun onEvent(event: AnnotationCreationEvent) {
        when (event) {
            is AnnotationCreationEvent.OnInitWithSelection -> onInitWithSelection(event)
            is AnnotationCreationEvent.OnInitWithAnnotation -> onInitWithAnnotation(event)
            is AnnotationCreationEvent.OnSelectNewColor -> onSelectNewColor(event)
            is AnnotationCreationEvent.OnChangeNote -> onChangeNote(event)
            is AnnotationCreationEvent.OnSave -> onSave(event)
            is AnnotationCreationEvent.OnDelete -> onDelete(event)
        }
    }

    private fun onInitWithSelection(event: AnnotationCreationEvent.OnInitWithSelection) {
        if (isInitialized) return
        isInitialized = true

        updateState {
            it.copy(
                selectionDraft = event.draft,
                annotation = null,
                selectedColor = it.selectedColor,
                note = "",
                isLoading = false,
            )
        }
    }

    private fun onInitWithAnnotation(event: AnnotationCreationEvent.OnInitWithAnnotation) {
        if (isInitialized) return
        isInitialized = true
        if (currentState().annotation?.id == event.annotationId) return

        updateState { it.copy(isLoading = true) }

        launch {
            val entity = AnnotationManager.getAnnotation(event.bookmarkId, event.annotationId)
            if (entity != null) {
                val color = if (entity.colorRole == YabaColor.NONE) {
                    YabaColor.YELLOW
                } else {
                    entity.colorRole
                }
                updateState {
                    it.copy(
                        selectionDraft = null,
                        annotation = mapEntityToUiModel(entity),
                        bookmarkIdForEdit = event.bookmarkId,
                        selectedColor = color,
                        note = entity.note ?: "",
                        isLoading = false,
                    )
                }
            } else {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    private fun mapEntityToUiModel(entity: AnnotationEntity) =
        AnnotationUiModel(
            id = entity.id,
            type = entity.type,
            colorRole = entity.colorRole,
            note = entity.note,
            quoteText = entity.quoteText,
            extrasJson = entity.extrasJson,
            createdAt = entity.createdAt,
            editedAt = entity.editedAt,
        )

    private fun onSelectNewColor(event: AnnotationCreationEvent.OnSelectNewColor) {
        updateState { it.copy(selectedColor = event.newColor) }
    }

    private fun onChangeNote(event: AnnotationCreationEvent.OnChangeNote) {
        updateState { it.copy(note = event.note) }
    }

    private fun onSave(event: AnnotationCreationEvent.OnSave) {
        val state = currentState()
        if (state.isSaving || !state.hasValidSelection) {
            event.onErrorCallback(IllegalStateException("Cannot save: invalid state"))
            return
        }

        updateState { it.copy(isSaving = true) }

        launch {
            try {
                if (state.annotation != null) {
                    val bookmarkId = state.bookmarkIdForEdit ?: return@launch
                    AnnotationManager.updateAnnotation(
                        bookmarkId = bookmarkId,
                        annotationId = state.annotation.id,
                        colorRole = state.selectedColor,
                        note = state.note.ifBlank { null },
                    )
                } else {
                    val draft = state.selectionDraft ?: return@launch
                    val annotationType = draft.sourceContext.type
                    val (extrasJson, typeForDb) = when (annotationType) {
                        AnnotationType.PDF -> {
                            val anchor = draft.pdfAnchor ?: run {
                                updateState { it.copy(isSaving = false) }
                                event.onErrorCallback(IllegalStateException("Expected PDF selection anchor"))
                                return@launch
                            }
                            json.encodeToString(PdfAnnotationExtras.serializer(), anchor) to AnnotationType.PDF
                        }
                        AnnotationType.EPUB -> {
                            val anchor = draft.epubAnchor ?: run {
                                updateState { it.copy(isSaving = false) }
                                event.onErrorCallback(IllegalStateException("Expected EPUB selection anchor"))
                                return@launch
                            }
                            json.encodeToString(EpubAnnotationExtras.serializer(), anchor) to AnnotationType.EPUB
                        }
                        else -> {
                            updateState { it.copy(isSaving = false) }
                            event.onErrorCallback(IllegalStateException("Unsupported annotation type for docmark selection"))
                            return@launch
                        }
                    }
                    val annotationId = IdGenerator.newId()
                    AnnotationManager.createAnnotation(
                        annotationId = annotationId,
                        bookmarkId = draft.bookmarkId,
                        readableVersionId = draft.readableVersionId,
                        type = typeForDb,
                        colorRole = state.selectedColor,
                        note = state.note.ifBlank { null },
                        quoteText = draft.quote.displayText.ifBlank { null },
                        extrasJson = extrasJson,
                    )
                }
                updateState { it.copy(isSaving = false) }
                event.onSavedCallback()
            } catch (e: Exception) {
                updateState { it.copy(isSaving = false) }
                event.onErrorCallback(e)
            }
        }
    }

    private fun onDelete(event: AnnotationCreationEvent.OnDelete) {
        val state = currentState()
        val annotation = state.annotation ?: run {
            event.onErrorCallback(IllegalStateException("Cannot delete: not editing"))
            return
        }
        val bookmarkId = state.bookmarkIdForEdit ?: run {
            event.onErrorCallback(IllegalStateException("Cannot delete: no bookmark"))
            return
        }
        AnnotationManager.deleteAnnotation(bookmarkId = bookmarkId, annotationId = annotation.id)
        event.onDeletedCallback()
    }

    override fun clear() {
        isInitialized = false
        super.clear()
    }
}
