package dev.subfly.yaba.core.state.creation.notemark

import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel

sealed class NotemarkCreationEvent {
    data class OnInit(
        val notemarkIdString: String? = null,
        val initialFolderId: String? = null,
        val initialTagIds: List<String>? = null,
    ) : NotemarkCreationEvent()

    data object OnCyclePreviewAppearance : NotemarkCreationEvent()

    data class OnChangeLabel(val newLabel: String) : NotemarkCreationEvent()

    data class OnChangeDescription(val newDescription: String) : NotemarkCreationEvent()

    data class OnSelectFolder(val folder: FolderUiModel) : NotemarkCreationEvent()

    data class OnSelectTags(val tags: List<TagUiModel>) : NotemarkCreationEvent()

    data class OnSave(
        val onSavedCallback: (bookmarkId: String) -> Unit,
        val onErrorCallback: () -> Unit,
    ) : NotemarkCreationEvent()
}
