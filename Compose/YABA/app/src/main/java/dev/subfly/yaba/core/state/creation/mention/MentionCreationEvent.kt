package dev.subfly.yaba.core.state.creation.mention

sealed interface MentionCreationEvent {
    data class OnInit(
        val initialText: String = "",
        val initialBookmarkId: String? = null,
        val isEdit: Boolean = false,
        val editPos: Int? = null,
    ) : MentionCreationEvent

    data class OnChangeMentionText(
        val text: String,
    ) : MentionCreationEvent

    /**
     * User finished the bookmark picker; [bookmarkId] is the only payload from selection.
     * Mention text is auto-filled from the bookmark label when it is still blank.
     */
    data class OnBookmarkPickedFromSelection(
        val bookmarkId: String,
    ) : MentionCreationEvent
}
