package dev.subfly.yaba.core.state.creation.notemark

sealed interface NotemarkMentionCreationEvent {
    data class OnInit(
        val initialText: String = "",
        val initialBookmarkId: String? = null,
        val isEdit: Boolean = false,
        val editPos: Int? = null,
    ) : NotemarkMentionCreationEvent

    data class OnChangeMentionText(
        val text: String,
    ) : NotemarkMentionCreationEvent

    /**
     * User finished the bookmark picker; [bookmarkId] is the only payload from selection.
     * Mention text is auto-filled from the bookmark label when it is still blank.
     */
    data class OnBookmarkPickedFromSelection(
        val bookmarkId: String,
    ) : NotemarkMentionCreationEvent
}
