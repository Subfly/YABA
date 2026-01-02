package dev.subfly.yabacore.state.folder


import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FolderCreationStateMachine : BaseStateMachine<FolderCreationUIState, FolderCreationEvent>(
    initialState = FolderCreationUIState()
) {
    override fun onEvent(event: FolderCreationEvent) {
        when (event) {
            is FolderCreationEvent.OnInitWithFolder -> onInitWithFolder(event)
            is FolderCreationEvent.OnSelectNewParent -> onSelectNewParent(event)
            is FolderCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is FolderCreationEvent.OnChangeDescription -> onChangeDescription(event)
            is FolderCreationEvent.OnSelectNewColor -> onSelectNewColor(event)
            is FolderCreationEvent.OnSelectNewIcon -> onSelectNewIcon(event)
            FolderCreationEvent.OnSave -> onSave()
        }
    }

    private fun onInitWithFolder(event: FolderCreationEvent.OnInitWithFolder) {
        launch {
            event.folderIdString?.let { nonNullIdString ->
                val folderId = Uuid.parse(nonNullIdString)
                FolderManager.getFolder(folderId)?.let { nonNullFolder ->
                    updateState {
                        it.copy(
                            label = nonNullFolder.label,
                            description = nonNullFolder.description ?: "",
                            selectedColor = nonNullFolder.color,
                            selectedIcon = nonNullFolder.icon,
                            selectedParentId = nonNullFolder.parentId?.toString(),
                            editingFolder = nonNullFolder,
                        )
                    }
                }
            }
        }
    }

    private fun onSelectNewParent(event: FolderCreationEvent.OnSelectNewParent) {
        updateState {
            it.copy(
                selectedParentId = event.newParentId,
            )
        }
    }

    private fun onChangeLabel(event: FolderCreationEvent.OnChangeLabel) {
        updateState {
            it.copy(
                label = event.newLabel,
            )
        }
    }

    private fun onChangeDescription(event: FolderCreationEvent.OnChangeDescription) {
        updateState {
            it.copy(
                label = event.newDescription,
            )
        }
    }

    private fun onSelectNewColor(event: FolderCreationEvent.OnSelectNewColor) {
        updateState {
            it.copy(
                selectedColor = event.newColor,
            )
        }
    }

    private fun onSelectNewIcon(event: FolderCreationEvent.OnSelectNewIcon) {
        updateState {
            it.copy(
                selectedIcon = event.newIcon,
            )
        }
    }

    private fun onSave() {
        val currentState = currentState()
        launch {
            if (currentState.editingFolder == null) {
                val newParentId = if (currentState.selectedParentId != null) {
                    Uuid.parse(currentState.selectedParentId)
                } else {
                    null
                }
                FolderManager.createFolder(
                    FolderUiModel(
                        id = Uuid.generateV7(),
                        label = currentState.label,
                        icon = currentState.selectedIcon,
                        color = currentState.selectedColor,
                        parentId = newParentId,
                        description = currentState.description,
                        createdAt = Clock.System.now(),
                        editedAt = Clock.System.now(),
                        order = -1,
                    )
                )
            } else {
                val editingFolder = currentState.editingFolder
                FolderManager.updateFolder(
                    editingFolder.copy(
                        label = if (editingFolder.label != currentState.label) {
                            currentState.label
                        } else {
                            editingFolder.label
                        },
                        description = if (editingFolder.description != currentState.description) {
                            currentState.description
                        } else {
                            editingFolder.description
                        },
                        icon = if (editingFolder.icon != currentState.selectedIcon) {
                            currentState.selectedIcon
                        } else {
                            editingFolder.icon
                        },
                        color = if (editingFolder.color != currentState.selectedColor) {
                            currentState.selectedColor
                        } else {
                            editingFolder.color
                        },
                        parentId = if (editingFolder.parentId?.toString() != currentState.selectedParentId) {
                            currentState.selectedParentId?.let { Uuid.parse(it) }
                        } else {
                            editingFolder.parentId
                        },
                    )
                )
            }
        }
    }
}
