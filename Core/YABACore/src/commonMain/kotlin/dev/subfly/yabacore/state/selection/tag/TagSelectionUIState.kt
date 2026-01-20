package dev.subfly.yabacore.state.selection.tag

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.TagUiModel

@Immutable
data class TagSelectionUIState(
    /**
     * Tags that have been selected for the bookmark.
     * These appear in the "Selected Tags" section.
     */
    val selectedTags: List<TagUiModel> = emptyList(),

    /**
     * Tags available for selection (not yet selected), filtered by search query.
     * These appear in the "Selectable Tags" section.
     */
    val availableTags: List<TagUiModel> = emptyList(),

    /**
     * Current search query for filtering available tags by label.
     */
    val searchQuery: String = "",

    /**
     * Indicates whether tag data is being loaded.
     */
    val isLoading: Boolean = true,
) {
    /**
     * Whether there are no tags at all (none selected and none available).
     */
    val hasNoTags: Boolean
        get() = selectedTags.isEmpty() && availableTags.isEmpty() && searchQuery.isBlank()

    /**
     * Whether search returned no results (but there might be selected tags).
     */
    val searchHasNoResults: Boolean
        get() = availableTags.isEmpty() && searchQuery.isNotBlank()

    /**
     * Whether all available tags have been selected.
     */
    val allTagsSelected: Boolean
        get() = availableTags.isEmpty() && selectedTags.isNotEmpty() && searchQuery.isBlank()
}

