package dev.subfly.yaba.core.state.creation.notemark

import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.state.base.BaseStateMachine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

class NotemarkMentionCreationStateMachine :
    BaseStateMachine<NotemarkMentionCreationUIState, NotemarkMentionCreationEvent>(
        initialState = NotemarkMentionCreationUIState(),
    ) {
    private var isInitialized = false
    private var bookmarkObservationJob: Job? = null
    private var shouldAutofillMentionFromBookmarkLabel = false

    override fun onEvent(event: NotemarkMentionCreationEvent) {
        when (event) {
            is NotemarkMentionCreationEvent.OnInit -> onInit(event)
            is NotemarkMentionCreationEvent.OnChangeMentionText -> onChangeMentionText(event)
            is NotemarkMentionCreationEvent.OnBookmarkPickedFromSelection ->
                onBookmarkPickedFromSelection(event)
        }
    }

    private fun onInit(event: NotemarkMentionCreationEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true
        updateState {
            NotemarkMentionCreationUIState(
                mentionText = event.initialText,
                selectedBookmarkId = event.initialBookmarkId,
                isEdit = event.isEdit,
                editPos = event.editPos,
            )
        }
        subscribeBookmark(event.initialBookmarkId)
    }

    private fun onChangeMentionText(event: NotemarkMentionCreationEvent.OnChangeMentionText) {
        updateState { it.copy(mentionText = event.text) }
    }

    private fun onBookmarkPickedFromSelection(
        event: NotemarkMentionCreationEvent.OnBookmarkPickedFromSelection,
    ) {
        shouldAutofillMentionFromBookmarkLabel = true
        updateState { it.copy(selectedBookmarkId = event.bookmarkId) }
        subscribeBookmark(event.bookmarkId)
    }

    private fun subscribeBookmark(bookmarkId: String?) {
        bookmarkObservationJob?.cancel()
        if (bookmarkId == null) {
            updateState { it.copy(selectedBookmark = null) }
            return
        }
        bookmarkObservationJob = launch {
            AllBookmarksManager.observeBookmarkById(bookmarkId).collectLatest { model ->
                updateState { state ->
                    var text = state.mentionText
                    when {
                        shouldAutofillMentionFromBookmarkLabel &&
                            model != null &&
                            text.isBlank() -> {
                            text = model.label
                            shouldAutofillMentionFromBookmarkLabel = false
                        }
                        model != null -> shouldAutofillMentionFromBookmarkLabel = false
                        else -> shouldAutofillMentionFromBookmarkLabel = false
                    }
                    state.copy(
                        selectedBookmark = model,
                        mentionText = text,
                    )
                }
            }
        }
    }

    override fun clear() {
        isInitialized = false
        bookmarkObservationJob?.cancel()
        bookmarkObservationJob = null
        shouldAutofillMentionFromBookmarkLabel = false
        super.clear()
    }
}
