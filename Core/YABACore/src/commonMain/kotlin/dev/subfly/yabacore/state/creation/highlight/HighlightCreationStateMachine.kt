package dev.subfly.yabacore.state.creation.highlight

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.managers.HighlightManager
import dev.subfly.yabacore.model.highlight.HighlightType
import dev.subfly.yabacore.model.highlight.PdfHighlightExtras
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.serialization.json.Json

class HighlightCreationStateMachine :
    BaseStateMachine<HighlightCreationUIState, HighlightCreationEvent>(
        initialState = HighlightCreationUIState(),
    ) {
    private var isInitialized = false

    private val json = Json { ignoreUnknownKeys = true }

    override fun onEvent(event: HighlightCreationEvent) {
        when (event) {
            is HighlightCreationEvent.OnInitWithSelection -> onInitWithSelection(event)
            is HighlightCreationEvent.OnInitWithHighlight -> onInitWithHighlight(event)
            is HighlightCreationEvent.OnSelectNewColor -> onSelectNewColor(event)
            is HighlightCreationEvent.OnChangeNote -> onChangeNote(event)
            is HighlightCreationEvent.OnSave -> onSave(event)
            is HighlightCreationEvent.OnDelete -> onDelete(event)
        }
    }

    private fun onInitWithSelection(event: HighlightCreationEvent.OnInitWithSelection) {
        if (isInitialized) return
        isInitialized = true

        updateState {
            it.copy(
                selectionDraft = event.draft,
                highlight = null,
                selectedColor = it.selectedColor,
                note = "",
                isLoading = false,
            )
        }
    }

    private fun onInitWithHighlight(event: HighlightCreationEvent.OnInitWithHighlight) {
        if (isInitialized) return
        isInitialized = true
        if (currentState().highlight?.id == event.highlightId) return

        updateState { it.copy(isLoading = true) }

        launch {
            val entity = HighlightManager.getHighlight(event.bookmarkId, event.highlightId)
            if (entity != null) {
                val color = if (entity.colorRole == YabaColor.NONE) {
                    YabaColor.YELLOW
                } else {
                    entity.colorRole
                }
                updateState {
                    it.copy(
                        selectionDraft = null,
                        highlight = mapEntityToUiModel(entity),
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

    private fun mapEntityToUiModel(entity: HighlightEntity) =
        HighlightUiModel(
            id = entity.id,
            type = entity.type,
            colorRole = entity.colorRole,
            note = entity.note,
            quoteText = entity.quoteText,
            extrasJson = entity.extrasJson,
            absolutePath = null,
            createdAt = entity.createdAt,
            editedAt = entity.editedAt,
        )

    private fun onSelectNewColor(event: HighlightCreationEvent.OnSelectNewColor) {
        updateState { it.copy(selectedColor = event.newColor) }
    }

    private fun onChangeNote(event: HighlightCreationEvent.OnChangeNote) {
        updateState { it.copy(note = event.note) }
    }

    private fun onSave(event: HighlightCreationEvent.OnSave) {
        val state = currentState()
        if (state.isSaving || !state.hasValidSelection) {
            event.onErrorCallback(IllegalStateException("Cannot save: invalid state"))
            return
        }

        updateState { it.copy(isSaving = true) }

        launch {
            try {
                if (state.highlight != null) {
                    val bookmarkId = state.bookmarkIdForEdit ?: return@launch
                    HighlightManager.updateHighlight(
                        bookmarkId = bookmarkId,
                        highlightId = state.highlight.id,
                        colorRole = state.selectedColor,
                        note = state.note.ifBlank { null },
                    )
                } else {
                    val draft = state.selectionDraft ?: return@launch
                    val anchor = draft.pdfAnchor ?: run {
                        updateState { it.copy(isSaving = false) }
                        event.onErrorCallback(IllegalStateException("Expected PDF selection anchor"))
                        return@launch
                    }
                    val highlightId = IdGenerator.newId()
                    val extrasJson = json.encodeToString(PdfHighlightExtras.serializer(), anchor)
                    HighlightManager.createHighlight(
                        highlightId = highlightId,
                        bookmarkId = draft.bookmarkId,
                        readableVersionId = draft.readableVersionId,
                        type = HighlightType.PDF,
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

    private fun onDelete(event: HighlightCreationEvent.OnDelete) {
        val state = currentState()
        val highlight = state.highlight ?: run {
            event.onErrorCallback(IllegalStateException("Cannot delete: not editing"))
            return
        }
        val bookmarkId = state.bookmarkIdForEdit ?: run {
            event.onErrorCallback(IllegalStateException("Cannot delete: no bookmark"))
            return
        }
        HighlightManager.deleteHighlight(bookmarkId = bookmarkId, highlightId = highlight.id)
        event.onDeletedCallback()
    }

    override fun clear() {
        isInitialized = false
        super.clear()
    }
}
