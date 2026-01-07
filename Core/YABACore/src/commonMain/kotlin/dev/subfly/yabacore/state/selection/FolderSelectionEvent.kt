package dev.subfly.yabacore.state.selection

import dev.subfly.yabacore.model.utils.FolderSelectionMode

sealed class FolderSelectionEvent {
    /**
     * Initialize folder selection with the given mode and context.
     *
     * @param mode The selection mode determining which folders to show.
     * @param contextFolderId The folder ID to use for filtering:
     * - FOLDER_SELECTION: Not used (can be null), shows all folders.
     * - PARENT_SELECTION: The folder being edited - excludes it, its current parent, and
     * descendants.
     * - BOOKMARK_MOVE: The bookmark's current folder - excludes it.
     */
    data class OnInit(
        val mode: FolderSelectionMode,
        val contextFolderId: String? = null,
    ) : FolderSelectionEvent()

    /** Update the search query to filter folders by label. */
    data class OnSearchQueryChanged(
        val query: String,
    ) : FolderSelectionEvent()
}
