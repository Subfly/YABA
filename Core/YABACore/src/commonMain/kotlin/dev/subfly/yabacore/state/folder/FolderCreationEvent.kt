package dev.subfly.yabacore.state.folder

import dev.subfly.yabacore.model.utils.YabaColor

sealed class FolderCreationEvent {
    data class OnInitWithFolder(val folderIdString: String?) : FolderCreationEvent()
    data class OnSelectNewParent(val newParentId: String?) : FolderCreationEvent()
    data class OnSelectNewColor(val newColor: YabaColor) : FolderCreationEvent()
    data class OnSelectNewIcon(val newIcon: String) : FolderCreationEvent()
    data class OnChangeLabel(val newLabel: String) : FolderCreationEvent()
    data class OnChangeDescription(val newDescription: String) : FolderCreationEvent()
    data object OnSave: FolderCreationEvent()
}