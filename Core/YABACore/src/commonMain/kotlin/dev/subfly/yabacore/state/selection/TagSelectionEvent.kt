package dev.subfly.yabacore.state.selection

import dev.subfly.yabacore.model.ui.TagUiModel

sealed class TagSelectionEvent {
    /**
     * Initialize tag selection with the IDs of already selected tags.
     *
     * @param selectedTagIds List of tag ID strings that are already selected for the bookmark.
     *                       These will be shown in the "Selected Tags" section.
     */
    data class OnInit(
        val selectedTagIds: List<String> = emptyList(),
    ) : TagSelectionEvent()

    /**
     * Update the search query to filter available (unselected) tags by label.
     */
    data class OnSearchQueryChanged(
        val query: String,
    ) : TagSelectionEvent()

    /**
     * Add a tag to the selection.
     * Moves the tag from available list to selected list.
     */
    data class OnSelectTag(
        val tag: TagUiModel,
    ) : TagSelectionEvent()

    /**
     * Remove a tag from the selection.
     * Moves the tag from selected list back to available list.
     */
    data class OnDeselectTag(
        val tag: TagUiModel,
    ) : TagSelectionEvent()
}

