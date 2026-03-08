package dev.subfly.yabacore.state.creation.imagemark

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.filesystem.ImagemarkFileManager
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.managers.ImagemarkManager
import dev.subfly.yabacore.filesystem.access.YabaFileAccessor
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.state.base.BaseStateMachine
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class ImagemarkCreationStateMachine :
    BaseStateMachine<ImagemarkCreationUIState, ImagemarkCreationEvent>(
        initialState = ImagemarkCreationUIState(),
    ) {
    private var isInitialized = false

    override fun onEvent(event: ImagemarkCreationEvent) {
        when (event) {
            is ImagemarkCreationEvent.OnInit -> onInit(event)
            ImagemarkCreationEvent.OnPickFromGallery -> onPickFromGallery()
            is ImagemarkCreationEvent.OnImageFromShare -> onImageFromShare(event)
            ImagemarkCreationEvent.OnCaptureFromCamera -> onCaptureFromCamera()
            ImagemarkCreationEvent.OnClearImage -> onClearImage()
            is ImagemarkCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is ImagemarkCreationEvent.OnChangeDescription -> onChangeDescription(event)
            is ImagemarkCreationEvent.OnChangeSummary -> onChangeSummary(event)
            is ImagemarkCreationEvent.OnSelectFolder -> onSelectFolder(event)
            is ImagemarkCreationEvent.OnSelectTags -> onSelectTags(event)
            is ImagemarkCreationEvent.OnSave -> onSave(event)
        }
    }

    private fun onInit(event: ImagemarkCreationEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true

        launch {
            event.imagemarkIdString?.let { imagemarkId ->
                val existing = ImagemarkManager.getImagemarkDetail(imagemarkId)
                if (existing != null) {
                    val savedBytes = ImagemarkFileManager.readImageBytes(imagemarkId)
                    val ext = existing.localImagePath?.substringAfterLast('.') ?: "jpeg"
                    updateState {
                        it.copy(
                            label = existing.label,
                            description = existing.description ?: "",
                            summary = existing.summary ?: "",
                            selectedFolder = existing.parentFolder,
                            selectedTags = existing.tags,
                            editingImagemark = existing,
                            imageBytes = savedBytes,
                            imageExtension = ext,
                        )
                    }
                }
            }

            event.initialFolderId?.let { folderId ->
                val folder = FolderManager.getFolder(folderId)
                if (folder != null) {
                    updateState { it.copy(selectedFolder = folder) }
                }
            }

            event.initialTagIds?.let { tagIds ->
                val tags = tagIds.mapNotNull { tagId -> TagManager.getTag(tagId) }
                if (tags.isNotEmpty()) {
                    updateState { it.copy(selectedTags = tags) }
                }
            }

            if (currentState().selectedFolder == null) {
                val temporaryUncategorized = FolderManager.createUncategorizedFolderModel()
                updateState {
                    it.copy(
                        selectedFolder = temporaryUncategorized,
                        uncategorizedFolderCreationRequired = true,
                    )
                }
            }
        }
    }

    private fun onImageFromShare(event: ImagemarkCreationEvent.OnImageFromShare) {
        updateState {
            it.copy(
                imageBytes = event.bytes,
                imageExtension = event.extension.lowercase().removePrefix(".").ifBlank { "jpeg" },
                error = null,
            )
        }
    }

    private fun onPickFromGallery() {
        launch {
            updateState { it.copy(isLoading = true, error = null) }
            try {
                val file = YabaFileAccessor.pickSingleImage() ?: run {
                    updateState { it.copy(isLoading = false) }
                    return@launch
                }
                val bytes = withContext(Dispatchers.IO) {
                    file.readBytes()
                }
                val ext = file.extension
                    .lowercase()
                    .removePrefix(".")
                    .ifBlank { "jpeg" }

                updateState {
                    it.copy(
                        imageBytes = bytes,
                        imageExtension = ext,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (_: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = ImagemarkCreationError.ImageReadFailed,
                    )
                }
            }
        }
    }

    private fun onCaptureFromCamera() {
        launch {
            updateState { it.copy(isLoading = true, error = null) }
            try {
                val file = YabaFileAccessor.capturePhoto() ?: run {
                    updateState { it.copy(isLoading = false) }
                    return@launch
                }
                val bytes = withContext(Dispatchers.IO) {
                    file.readBytes()
                }
                val ext = file.extension
                    .lowercase()
                    .removePrefix(".")
                    .ifBlank { "jpeg" }

                updateState {
                    it.copy(
                        imageBytes = bytes,
                        imageExtension = ext,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (_: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = ImagemarkCreationError.ImageReadFailed,
                    )
                }
            }
        }
    }

    private fun onClearImage() {
        updateState {
            it.copy(
                imageBytes = null,
                imageExtension = "jpeg",
            )
        }
    }

    private fun onChangeLabel(event: ImagemarkCreationEvent.OnChangeLabel) {
        updateState { it.copy(label = event.newLabel) }
    }

    private fun onChangeDescription(event: ImagemarkCreationEvent.OnChangeDescription) {
        updateState { it.copy(description = event.newDescription) }
    }

    private fun onChangeSummary(event: ImagemarkCreationEvent.OnChangeSummary) {
        updateState { it.copy(summary = event.newSummary) }
    }

    private fun onSelectFolder(event: ImagemarkCreationEvent.OnSelectFolder) {
        updateState {
            it.copy(
                selectedFolder = event.folder,
                uncategorizedFolderCreationRequired = false,
            )
        }
    }

    private fun onSelectTags(event: ImagemarkCreationEvent.OnSelectTags) {
        updateState { it.copy(selectedTags = event.tags) }
    }

    private fun onSave(event: ImagemarkCreationEvent.OnSave) {
        val state = currentState()
        var selectedFolder = state.selectedFolder ?: return

        if (state.imageBytes == null) {
            updateState { it.copy(error = ImagemarkCreationError.NoImage) }
            event.onErrorCallback()
            return
        }

        launch {
            updateState { it.copy(isSaving = true, error = null) }

            try {
                if (state.uncategorizedFolderCreationRequired) {
                    selectedFolder = FolderManager.ensureUncategorizedFolderVisible()
                }

                val bookmarkId = if (state.editingImagemark != null) {
                    state.editingImagemark.id
                } else {
                    IdGenerator.newId()
                }

                val imageBytes = state.imageBytes!!
                val imageExtension = state.imageExtension

                if (state.editingImagemark != null) {
                    AllBookmarksManager.updateBookmarkMetadata(
                        bookmarkId = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.IMAGE,
                        label = state.label,
                        description = state.description.ifBlank { null },
                        tagIds = state.selectedTags.map { it.id },
                        previewImageBytes = imageBytes,
                        previewImageExtension = imageExtension,
                        previewIconBytes = null,
                    )

                    val existingTagIds = state.editingImagemark.tags.map { it.id }.toSet()
                    val newTagIds = state.selectedTags.map { it.id }.toSet()

                    state.selectedTags.filter { it.id !in existingTagIds }.forEach { tag ->
                        AllBookmarksManager.addTagToBookmark(tag.id, bookmarkId)
                    }

                    state.editingImagemark.tags.filter { it.id !in newTagIds }.forEach { tag ->
                        AllBookmarksManager.removeTagFromBookmark(tag.id, bookmarkId)
                    }
                } else {
                    AllBookmarksManager.createBookmarkMetadata(
                        id = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.IMAGE,
                        label = state.label,
                        description = state.description.ifBlank { null },
                        tagIds = state.selectedTags.map { it.id },
                        previewImageBytes = imageBytes,
                        previewImageExtension = imageExtension,
                        previewIconBytes = null,
                    )
                }

                ImagemarkManager.createOrUpdateImageDetails(
                    bookmarkId = bookmarkId,
                    summary = state.summary.ifBlank { null },
                )

                updateState { it.copy(isSaving = false, error = null) }
                event.onSavedCallback()
            } catch (_: Exception) {
                updateState {
                    it.copy(
                        isSaving = false,
                        error = ImagemarkCreationError.SaveFailed,
                    )
                }
                event.onErrorCallback()
            }
        }
    }

    override fun clear() {
        isInitialized = false
        super.clear()
    }
}
