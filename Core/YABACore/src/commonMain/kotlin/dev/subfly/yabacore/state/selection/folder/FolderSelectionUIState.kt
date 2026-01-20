package dev.subfly.yabacore.state.selection.folder

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.FolderUiModel

@Immutable
data class FolderSelectionUIState(
    /** Flat list of folders to display, already filtered based on mode and search query. */
    val folders: List<FolderUiModel> = emptyList(),

    /** Current search query for filtering folders by label. */
    val searchQuery: String = "",

    /** Indicates whether folder data is being loaded. */
    val isLoading: Boolean = true,

    /**
     * For PARENT_SELECTION mode: indicates if the current folder has a parent, meaning "Move to
     * Root" option should be shown.
     */
    val canMoveToRoot: Boolean = false,
)
