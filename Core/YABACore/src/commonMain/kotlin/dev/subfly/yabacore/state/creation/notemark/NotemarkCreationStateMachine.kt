package dev.subfly.yabacore.state.creation.notemark

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.managers.NotemarkManager
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.state.base.BaseStateMachine

class NotemarkCreationStateMachine :
    BaseStateMachine<NotemarkCreationUIState, NotemarkCreationEvent>(
        initialState = NotemarkCreationUIState(),
    ) {
    private var isInitialized = false
    private val preferencesStore
        get() = SettingsStores.userPreferences

    override fun onEvent(event: NotemarkCreationEvent) {
        when (event) {
            is NotemarkCreationEvent.OnInit -> onInit(event)
            NotemarkCreationEvent.OnCyclePreviewAppearance -> onCyclePreviewAppearance()
            is NotemarkCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is NotemarkCreationEvent.OnChangeDescription -> onChangeDescription(event)
            is NotemarkCreationEvent.OnSelectFolder -> onSelectFolder(event)
            is NotemarkCreationEvent.OnSelectTags -> onSelectTags(event)
            is NotemarkCreationEvent.OnSave -> onSave(event)
        }
    }

    private fun onInit(event: NotemarkCreationEvent.OnInit) {
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

            event.notemarkIdString?.let { id ->
                val existing = NotemarkManager.getNotemarkDetail(id)
                if (existing != null) {
                    updateState {
                        it.copy(
                            label = existing.label,
                            description = existing.description.orEmpty(),
                            selectedFolder = existing.parentFolder,
                            selectedTags = existing.tags,
                            editingNotemark = existing,
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

    private fun onChangeLabel(event: NotemarkCreationEvent.OnChangeLabel) {
        updateState { it.copy(label = event.newLabel) }
    }

    private fun onChangeDescription(event: NotemarkCreationEvent.OnChangeDescription) {
        updateState { it.copy(description = event.newDescription) }
    }

    private fun onSelectFolder(event: NotemarkCreationEvent.OnSelectFolder) {
        updateState {
            it.copy(
                selectedFolder = event.folder,
                uncategorizedFolderCreationRequired = false,
            )
        }
    }

    private fun onSelectTags(event: NotemarkCreationEvent.OnSelectTags) {
        updateState { it.copy(selectedTags = event.tags) }
    }

    private fun onSave(event: NotemarkCreationEvent.OnSave) {
        val state = currentState()
        var selectedFolder = state.selectedFolder ?: return

        if (state.label.isBlank()) {
            updateState { it.copy(error = NotemarkCreationError.LabelRequired) }
            event.onErrorCallback()
            return
        }

        launch {
            updateState { it.copy(isSaving = true, error = null) }

            try {
                if (state.uncategorizedFolderCreationRequired) {
                    selectedFolder = FolderManager.ensureUncategorizedFolderVisible()
                }

                val bookmarkId = state.editingNotemark?.id ?: IdGenerator.newId()
                val label = state.label.trim()

                if (state.editingNotemark != null) {
                    AllBookmarksManager.updateBookmarkMetadata(
                        bookmarkId = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.NOTE,
                        label = label,
                        description = state.description.ifBlank { null },
                        tagIds = state.selectedTags.map { it.id },
                    )

                    val existingTagIds = state.editingNotemark.tags.map { it.id }.toSet()
                    val newTagIds = state.selectedTags.map { it.id }.toSet()
                    state.selectedTags.filter { it.id !in existingTagIds }.forEach { tag ->
                        AllBookmarksManager.addTagToBookmark(tag.id, bookmarkId)
                    }
                    state.editingNotemark.tags.filter { it.id !in newTagIds }.forEach { tag ->
                        AllBookmarksManager.removeTagFromBookmark(tag.id, bookmarkId)
                    }

                    NotemarkManager.createOrUpdateNoteDetails(bookmarkId = bookmarkId)
                } else {
                    AllBookmarksManager.createBookmarkMetadata(
                        id = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.NOTE,
                        label = label,
                        description = state.description.ifBlank { null },
                        tagIds = state.selectedTags.map { it.id },
                        previewImageBytes = null,
                        previewImageExtension = "jpeg",
                        previewIconBytes = null,
                    )
                    NotemarkManager.createOrUpdateNoteDetails(bookmarkId = bookmarkId)
                }

                updateState { it.copy(isSaving = false, error = null) }
                event.onSavedCallback(bookmarkId)
            } catch (_: Exception) {
                updateState {
                    it.copy(
                        isSaving = false,
                        error = NotemarkCreationError.SaveFailed,
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
