package dev.subfly.yabacore.state.selection.tag

import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

class TagSelectionStateMachine :
    BaseStateMachine<TagSelectionUIState, TagSelectionEvent>(
        initialState = TagSelectionUIState()
    ) {

    private var isInitialized = false
    private var tagSubscriptionJob: Job? = null

    // IDs of initially selected tags (from OnInit)
    private var selectedTagIds: Set<String> = emptySet()

    // In-memory list of currently selected tags (can change during session)
    private var currentSelectedTags: List<TagUiModel> = emptyList()

    // All tags from database (unfiltered)
    private var allTags: List<TagUiModel> = emptyList()

    override fun onEvent(event: TagSelectionEvent) {
        when (event) {
            is TagSelectionEvent.OnInit -> onInit(event)
            is TagSelectionEvent.OnSearchQueryChanged -> onSearchQueryChanged(event)
            is TagSelectionEvent.OnSelectTag -> onSelectTag(event)
            is TagSelectionEvent.OnDeselectTag -> onDeselectTag(event)
        }
    }

    private fun onInit(event: TagSelectionEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true

        // Tag IDs are already strings
        selectedTagIds = event.selectedTagIds.toSet()

        launch {
            updateState { it.copy(isLoading = true) }
            startTagObservation()
        }
    }

    private fun startTagObservation() {
        tagSubscriptionJob?.cancel()
        tagSubscriptionJob = launch {
            TagManager.observeTags(
                sortType = SortType.LABEL,
                sortOrder = SortOrderType.ASCENDING
            ).collectLatest { tags ->
                allTags = tags

                // Separate into selected and available based on current selection state
                // On first load, use selectedTagIds; after that, use currentSelectedTags
                if (currentSelectedTags.isEmpty() && selectedTagIds.isNotEmpty()) {
                    // Initial load: populate selected tags from IDs
                    currentSelectedTags = tags.filter { it.id in selectedTagIds }
                } else {
                    // Subsequent updates: keep current selection but update with fresh data
                    val currentIds = currentSelectedTags.map { it.id }.toSet()
                    currentSelectedTags = tags.filter { it.id in currentIds }
                }

                updateFilteredState()
            }
        }
    }

    private fun onSearchQueryChanged(event: TagSelectionEvent.OnSearchQueryChanged) {
        updateState { it.copy(searchQuery = event.query) }
        updateFilteredState()
    }

    private fun onSelectTag(event: TagSelectionEvent.OnSelectTag) {
        // Add to selected if not already there
        if (currentSelectedTags.none { it.id == event.tag.id }) {
            currentSelectedTags = currentSelectedTags + event.tag
            updateFilteredState()
        }
    }

    private fun onDeselectTag(event: TagSelectionEvent.OnDeselectTag) {
        // Remove from selected
        currentSelectedTags = currentSelectedTags.filter { it.id != event.tag.id }
        updateFilteredState()
    }

    /**
     * Updates the UI state with current selected and available tags.
     * Applies search filter to available tags.
     */
    private fun updateFilteredState() {
        val selectedIds = currentSelectedTags.map { it.id }.toSet()

        // Available = all tags minus selected
        var available = allTags.filter { it.id !in selectedIds }

        // Apply search filter to available tags
        val query = currentState().searchQuery
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            available = available.filter { tag ->
                tag.label.lowercase().contains(lowerQuery)
            }
        }

        updateState { state ->
            state.copy(
                selectedTags = currentSelectedTags,
                availableTags = available,
                isLoading = false
            )
        }
    }

    /**
     * Returns the list of currently selected tag IDs.
     * This can be used when saving the bookmark.
     */
    fun getSelectedTagIds(): List<String> =
        currentSelectedTags.map { it.id }

    /**
     * Returns the list of currently selected tags.
     * This can be used when returning to the bookmark creation screen.
     */
    fun getSelectedTags(): List<TagUiModel> = currentSelectedTags

    override fun clear() {
        isInitialized = false
        tagSubscriptionJob?.cancel()
        tagSubscriptionJob = null
        selectedTagIds = emptySet()
        currentSelectedTags = emptyList()
        allTags = emptyList()
        super.clear()
    }
}
