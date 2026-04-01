package dev.subfly.yaba.core.state.creation.canvmark

import dev.subfly.yaba.core.common.IdGenerator
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.managers.CanvmarkManager
import dev.subfly.yaba.core.managers.FolderManager
import dev.subfly.yaba.core.managers.TagManager
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.state.base.BaseStateMachine

class CanvmarkCreationStateMachine :
    BaseStateMachine<CanvmarkCreationUIState, CanvmarkCreationEvent>(
        initialState = CanvmarkCreationUIState(),
    ) {
    private var isInitialized = false
    private val preferencesStore
        get() = SettingsStores.userPreferences

    override fun onEvent(event: CanvmarkCreationEvent) {
        when (event) {
            is CanvmarkCreationEvent.OnInit -> onInit(event)
            CanvmarkCreationEvent.OnCyclePreviewAppearance -> onCyclePreviewAppearance()
            is CanvmarkCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is CanvmarkCreationEvent.OnChangeDescription -> onChangeDescription(event)
            is CanvmarkCreationEvent.OnSelectFolder -> onSelectFolder(event)
            is CanvmarkCreationEvent.OnSelectTags -> onSelectTags(event)
            is CanvmarkCreationEvent.OnSave -> onSave(event)
            CanvmarkCreationEvent.OnTogglePrivate -> onTogglePrivate()
            CanvmarkCreationEvent.OnTogglePinned -> onTogglePinned()
        }
    }

    private fun onInit(event: CanvmarkCreationEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true

        launch {
            val preferences = preferencesStore.get()
            updateState {
                it.copy(
                    bookmarkAppearance = preferences.preferredBookmarkAppearance,
                    cardImageSizing = preferences.preferredCardImageSizing,
                )
            }

            event.canvmarkIdString?.let { id ->
                val existing = CanvmarkManager.getCanvmarkDetail(id)
                if (existing != null) {
                    updateState {
                        it.copy(
                            label = existing.label,
                            description = existing.description.orEmpty(),
                            selectedFolder = existing.parentFolder,
                            selectedTags = existing.tags,
                            editingCanvmark = existing,
                            isPrivate = existing.isPrivate,
                            isPinned = existing.isPinned,
                        )
                    }
                }
            }

            event.initialFolderId?.let { folderId ->
                val folder = FolderManager.getFolder(folderId)
                if (folder != null) updateState { it.copy(selectedFolder = folder) }
            }

            event.initialTagIds?.let { tagIds ->
                val tags = tagIds.mapNotNull { tagId -> TagManager.getTag(tagId) }
                if (tags.isNotEmpty()) updateState { it.copy(selectedTags = tags) }
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

    private fun onCyclePreviewAppearance() {
        val state = currentState()
        val (nextAppearance, nextSizing) = when (state.bookmarkAppearance) {
            BookmarkAppearance.LIST -> BookmarkAppearance.CARD to CardImageSizing.SMALL
            BookmarkAppearance.CARD -> when (state.cardImageSizing) {
                CardImageSizing.SMALL -> BookmarkAppearance.CARD to CardImageSizing.BIG
                CardImageSizing.BIG -> BookmarkAppearance.GRID to state.cardImageSizing
            }

            BookmarkAppearance.GRID -> BookmarkAppearance.LIST to state.cardImageSizing
        }
        updateState {
            it.copy(
                bookmarkAppearance = nextAppearance,
                cardImageSizing = nextSizing,
            )
        }
    }

    private fun onChangeLabel(event: CanvmarkCreationEvent.OnChangeLabel) {
        updateState { it.copy(label = event.newLabel) }
    }

    private fun onChangeDescription(event: CanvmarkCreationEvent.OnChangeDescription) {
        updateState { it.copy(description = event.newDescription) }
    }

    private fun onSelectFolder(event: CanvmarkCreationEvent.OnSelectFolder) {
        updateState {
            it.copy(
                selectedFolder = event.folder,
                uncategorizedFolderCreationRequired = false,
            )
        }
    }

    private fun onSelectTags(event: CanvmarkCreationEvent.OnSelectTags) {
        updateState { it.copy(selectedTags = event.tags) }
    }

    private fun onSave(event: CanvmarkCreationEvent.OnSave) {
        val state = currentState()
        var selectedFolder = state.selectedFolder ?: return

        if (state.label.isBlank()) {
            updateState { it.copy(error = CanvmarkCreationError.LabelRequired) }
            event.onErrorCallback()
            return
        }

        launch {
            updateState { it.copy(isSaving = true, error = null) }

            try {
                if (state.uncategorizedFolderCreationRequired) {
                    selectedFolder = FolderManager.ensureUncategorizedFolderVisible()
                }

                val bookmarkId = state.editingCanvmark?.id ?: IdGenerator.newId()
                val label = state.label.trim()

                if (state.editingCanvmark != null) {
                    AllBookmarksManager.updateBookmarkMetadata(
                        bookmarkId = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.CANVAS,
                        label = label,
                        description = state.description.ifBlank { null },
                        isPrivate = state.isPrivate,
                        isPinned = state.isPinned,
                        tagIds = state.selectedTags.map { it.id },
                    )

                    val existingTagIds = state.editingCanvmark.tags.map { it.id }.toSet()
                    val newTagIds = state.selectedTags.map { it.id }.toSet()
                    state.selectedTags.filter { it.id !in existingTagIds }.forEach { tag ->
                        AllBookmarksManager.addTagToBookmark(tag.id, bookmarkId)
                    }
                    state.editingCanvmark.tags.filter { it.id !in newTagIds }.forEach { tag ->
                        AllBookmarksManager.removeTagFromBookmark(tag.id, bookmarkId)
                    }
                } else {
                    AllBookmarksManager.createBookmarkMetadata(
                        id = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.CANVAS,
                        label = label,
                        description = state.description.ifBlank { null },
                        isPrivate = state.isPrivate,
                        isPinned = state.isPinned,
                        tagIds = state.selectedTags.map { it.id },
                        previewImageBytes = null,
                        previewImageExtension = "jpeg",
                        previewIconBytes = null,
                    )
                }
                CanvmarkManager.createOrUpdateCanvasDetails(bookmarkId = bookmarkId)

                updateState { it.copy(isSaving = false, error = null) }
                event.onSavedCallback(bookmarkId)
            } catch (_: Exception) {
                updateState {
                    it.copy(
                        isSaving = false,
                        error = CanvmarkCreationError.SaveFailed,
                    )
                }
                event.onErrorCallback()
            }
        }
    }

    private fun onTogglePrivate() {
        updateState { it.copy(isPrivate = !it.isPrivate) }
    }

    private fun onTogglePinned() {
        updateState { it.copy(isPinned = !it.isPinned) }
    }

    override fun clear() {
        isInitialized = false
        super.clear()
    }
}
