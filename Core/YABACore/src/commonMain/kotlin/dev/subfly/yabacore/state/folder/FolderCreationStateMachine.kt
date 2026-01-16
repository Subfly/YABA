package dev.subfly.yabacore.state.folder

import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.Job
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FolderCreationStateMachine :
    BaseStateMachine<FolderCreationUIState, FolderCreationEvent>(
        initialState = FolderCreationUIState()
    ) {
    private var isInitialized = false
    private var parentFolderSubscriptionJob: Job? = null

    override fun onEvent(event: FolderCreationEvent) {
        when (event) {
            is FolderCreationEvent.OnInitWithFolder -> onInitWithFolder(event)
            is FolderCreationEvent.OnSelectNewParent -> onSelectNewParent(event)
            is FolderCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is FolderCreationEvent.OnChangeDescription -> onChangeDescription(event)
            is FolderCreationEvent.OnSelectNewColor -> onSelectNewColor(event)
            is FolderCreationEvent.OnSelectNewIcon -> onSelectNewIcon(event)
            is FolderCreationEvent.OnSave -> onSave(event)
        }
    }

    private fun onInitWithFolder(event: FolderCreationEvent.OnInitWithFolder) {
        if (isInitialized) return
        isInitialized = true

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
                            editingFolder = nonNullFolder,
                        )
                    }
                    // Start observing the parent folder if it exists
                    nonNullFolder.parentId?.let { parentId ->
                        onEvent(FolderCreationEvent.OnSelectNewParent(parentId.toString()))
                    }
                }
            }
        }
    }

    private fun onSelectNewParent(event: FolderCreationEvent.OnSelectNewParent) {
        // Cancel any existing parent folder subscription
        parentFolderSubscriptionJob?.cancel()
        parentFolderSubscriptionJob = null

        if (event.newParentId == null) {
            updateState { it.copy(selectedParent = null) }
            return
        }

        val folderId = Uuid.parse(event.newParentId)
        parentFolderSubscriptionJob = launch {
            FolderManager.observeFolder(folderId).collect { folder ->
                updateState { it.copy(selectedParent = folder) }
            }
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
                description = event.newDescription,
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

    private fun onSave(event: FolderCreationEvent.OnSave) {
        val currentState = currentState()
        if (currentState.isSaving) return
        updateState { it.copy(isSaving = true) }

        launch {
            try {
                if (currentState.editingFolder == null) {
                    FolderManager.createFolder(
                        FolderUiModel(
                            id = Uuid.generateV7(),
                            label = currentState.label,
                            icon = currentState.selectedIcon,
                            color = currentState.selectedColor,
                            parentId = currentState.selectedParent?.id,
                            description = currentState.description,
                            createdAt = Clock.System.now(),
                            editedAt = Clock.System.now(),
                            order = -1,
                        )
                    )
                } else {
                    val editingFolder = currentState.editingFolder
                    val newParentId = currentState.selectedParent?.id
                    val parentChanged = editingFolder.parentId != newParentId

                    // If parent changed, move the folder first (handles reordering)
                    if (parentChanged) {
                        FolderManager.moveFolder(editingFolder, currentState.selectedParent)
                    }

                    // Update metadata (label, description, icon, color)
                    FolderManager.updateFolder(
                        editingFolder.copy(
                            label = currentState.label,
                            description = currentState.description,
                            icon = currentState.selectedIcon,
                            color = currentState.selectedColor,
                        )
                    )
                }
                event.onSavedCallback()
            } finally {
                updateState { it.copy(isSaving = false) }
            }
        }
    }

    override fun clear() {
        isInitialized = false
        parentFolderSubscriptionJob?.cancel()
        parentFolderSubscriptionJob = null
        super.clear()
    }
}
