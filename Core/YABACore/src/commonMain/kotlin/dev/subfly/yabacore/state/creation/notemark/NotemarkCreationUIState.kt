package dev.subfly.yabacore.state.creation.notemark

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.NotemarkUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing

@Stable
data class NotemarkCreationUIState(
    val label: String = "",
    val description: String = "",
    val selectedFolder: FolderUiModel? = null,
    val selectedTags: List<TagUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: NotemarkCreationError? = null,
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val editingNotemark: NotemarkUiModel? = null,
    val uncategorizedFolderCreationRequired: Boolean = false,
) {
    val isInEditMode: Boolean
        get() = editingNotemark != null

    val canSave: Boolean
        get() = label.isNotBlank() && selectedFolder != null && !isLoading
}
