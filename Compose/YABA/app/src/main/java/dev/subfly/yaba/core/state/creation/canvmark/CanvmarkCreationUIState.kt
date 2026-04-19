package dev.subfly.yaba.core.state.creation.canvmark

import androidx.compose.runtime.Stable
import dev.subfly.yaba.core.model.ui.CanvmarkUiModel
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing

@Stable
data class CanvmarkCreationUIState(
    val label: String = "",
    val description: String = "",
    val selectedFolder: FolderUiModel? = null,
    val selectedTags: List<TagUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: CanvmarkCreationError? = null,
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val editingCanvmark: CanvmarkUiModel? = null,
    val isPinned: Boolean = false,
    val uncategorizedFolderCreationRequired: Boolean = false,
) {
    val isInEditMode: Boolean
        get() = editingCanvmark != null

    val canSave: Boolean
        get() = label.isNotBlank() && selectedFolder != null && !isLoading
}
