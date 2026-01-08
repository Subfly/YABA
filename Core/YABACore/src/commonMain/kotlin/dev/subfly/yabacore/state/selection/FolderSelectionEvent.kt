package dev.subfly.yabacore.state.selection

import dev.subfly.yabacore.model.utils.FolderSelectionMode

sealed class FolderSelectionEvent {
    /**
     * Initialize folder selection with the given mode and context.
     *
     * @param mode The selection mode determining which folders to show.
     * @param contextFolderId The folder ID to use for filtering (for PARENT_SELECTION or
     * BOOKMARK_MOVE modes).
     * @param contextBookmarkId The bookmark ID for BOOKMARK_MOVE mode. Cannot be used together with
     * contextFolderId for moving purposes.
     */
    data class OnInit(
        val mode: FolderSelectionMode,
        val contextFolderId: String? = null,
        val contextBookmarkId: String? = null,
    ) : FolderSelectionEvent()

    /** Update the search query to filter folders by label. */
    data class OnSearchQueryChanged(
        val query: String,
    ) : FolderSelectionEvent()

    /**
     * Move the context folder to the selected target folder.
     *
     * @param targetFolderId The folder to move into. If null, moves to root level.
     */
    data class OnMoveFolderToSelected(
        val targetFolderId: String?,
    ) : FolderSelectionEvent()

    /**
     * Move the context bookmark to the selected target folder.
     *
     * @param targetFolderId The folder to move the bookmark into.
     */
    data class OnMoveBookmarkToSelected(
        val targetFolderId: String,
    ) : FolderSelectionEvent()
}
