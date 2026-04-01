package dev.subfly.yaba.core.state.creation.canvmark

import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel

sealed class CanvmarkCreationEvent {
    data class OnInit(
        val canvmarkIdString: String? = null,
        val initialFolderId: String? = null,
        val initialTagIds: List<String>? = null,
    ) : CanvmarkCreationEvent()

    data object OnCyclePreviewAppearance : CanvmarkCreationEvent()

    data class OnChangeLabel(val newLabel: String) : CanvmarkCreationEvent()

    data class OnChangeDescription(val newDescription: String) : CanvmarkCreationEvent()

    data class OnSelectFolder(val folder: FolderUiModel) : CanvmarkCreationEvent()

    data class OnSelectTags(val tags: List<TagUiModel>) : CanvmarkCreationEvent()

    data class OnSave(
        val onSavedCallback: (bookmarkId: String) -> Unit,
        val onErrorCallback: () -> Unit,
    ) : CanvmarkCreationEvent()

    data object OnTogglePrivate : CanvmarkCreationEvent()

    data object OnTogglePinned : CanvmarkCreationEvent()
}
