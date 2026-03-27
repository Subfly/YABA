package dev.subfly.yabacore.state.creation.docmark

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.filesystem.DocmarkFileManager
import dev.subfly.yabacore.filesystem.access.YabaFileAccessor
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.DocmarkManager
import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.DocmarkType
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.state.base.BaseStateMachine
import dev.subfly.yabacore.webview.WebShellLoadResult
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class DocmarkCreationStateMachine :
    BaseStateMachine<DocmarkCreationUIState, DocmarkCreationEvent>(
        initialState = DocmarkCreationUIState(),
    ) {
    private var isInitialized = false
    private val preferencesStore
        get() = SettingsStores.userPreferences

    override fun onEvent(event: DocmarkCreationEvent) {
        when (event) {
            is DocmarkCreationEvent.OnInit -> onInit(event)
            DocmarkCreationEvent.OnPickDocument -> onPickDocument()
            is DocmarkCreationEvent.OnDocumentFromShare -> onDocumentFromShare(event)
            DocmarkCreationEvent.OnClearDocument -> onClearDocument()
            DocmarkCreationEvent.OnCyclePreviewAppearance -> onCyclePreviewAppearance()
            is DocmarkCreationEvent.OnDocumentMetadataExtracted -> onDocumentMetadataExtracted(event)
            is DocmarkCreationEvent.OnSetGeneratedPreview -> onSetGeneratedPreview(event)
            is DocmarkCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is DocmarkCreationEvent.OnChangeDescription -> onChangeDescription(event)
            is DocmarkCreationEvent.OnChangeSummary -> onChangeSummary(event)
            is DocmarkCreationEvent.OnSelectFolder -> onSelectFolder(event)
            is DocmarkCreationEvent.OnSelectTags -> onSelectTags(event)
            is DocmarkCreationEvent.OnSave -> onSave(event)
            is DocmarkCreationEvent.OnWebInitialContentLoad -> onWebInitialContentLoad(event)
        }
    }

    private fun onInit(event: DocmarkCreationEvent.OnInit) {
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

            event.docmarkIdString?.let { docmarkId ->
                val existing = DocmarkManager.getDocmarkDetail(docmarkId)
                if (existing != null) {
                    updateState {
                        it.copy(
                            label = existing.label,
                            description = existing.description ?: "",
                            summary = existing.summary ?: "",
                            metadataTitle = existing.metadataTitle,
                            metadataDescription = existing.metadataDescription,
                            metadataAuthor = existing.metadataAuthor,
                            metadataDate = existing.metadataDate,
                            metadataIdentifier = existing.metadataIdentifier,
                            selectedFolder = existing.parentFolder,
                            selectedTags = existing.tags,
                            editingDocmark = existing,
                            docmarkType = existing.docmarkType,
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

    private fun onPickDocument() {
        if (currentState().isInEditMode) return
        launch {
            updateState { it.copy(isLoading = true, error = null) }
            try {
                val file =
                    YabaFileAccessor.pickSingleFile(extensions = listOf("pdf", "epub")) ?: run {
                        updateState { it.copy(isLoading = false) }
                        return@launch
                    }
                val bytes =
                    withContext(Dispatchers.IO) {
                        file.readBytes()
                    }
                val sourceName = file.name
                val docmarkType = inferDocmarkTypeFromFileName(sourceName)
                if (docmarkType == null) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = DocmarkCreationError.DocumentReadFailed,
                        )
                    }
                    return@launch
                }
                updateState {
                    it.copy(
                        documentBytes = bytes,
                        docmarkType = docmarkType,
                        sourceFileName = sourceName,
                        metadataTitle = null,
                        metadataDescription = null,
                        metadataAuthor = null,
                        metadataDate = null,
                        metadataIdentifier = null,
                        isLoading = true,
                        error = null,
                    )
                }
            } catch (_: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = DocmarkCreationError.DocumentReadFailed,
                    )
                }
            }
        }
    }

    private fun inferDocmarkTypeFromFileName(fileName: String): DocmarkType? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> DocmarkType.PDF
            "epub" -> DocmarkType.EPUB
            else -> null
        }
    }

    private fun onDocumentFromShare(event: DocmarkCreationEvent.OnDocumentFromShare) {
        if (currentState().isInEditMode) return
        val sourceFileName = event.sourceFileName?.trim()?.ifBlank { null }
        updateState { state ->
            state.copy(
                documentBytes = event.bytes,
                docmarkType = event.docmarkType,
                sourceFileName = sourceFileName,
                metadataTitle = null,
                metadataDescription = null,
                metadataAuthor = null,
                metadataDate = null,
                metadataIdentifier = null,
                previewImageBytes = null,
                previewImageExtension = "png",
                isLoading = true,
                error = null,
            )
        }
    }

    private fun onClearDocument() {
        if (currentState().isInEditMode) return
        updateState {
            it.copy(
                documentBytes = null,
                docmarkType = null,
                sourceFileName = null,
                metadataTitle = null,
                metadataDescription = null,
                metadataAuthor = null,
                metadataDate = null,
                metadataIdentifier = null,
                previewImageBytes = null,
                isLoading = false,
                error = null,
            )
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

    private fun onDocumentMetadataExtracted(event: DocmarkCreationEvent.OnDocumentMetadataExtracted) {
        updateState {
            it.copy(
                metadataTitle = event.metadataTitle,
                metadataDescription = event.metadataDescription,
                metadataAuthor = event.metadataAuthor,
                metadataDate = event.metadataDate,
                metadataIdentifier = event.metadataIdentifier,
            )
        }
    }

    private fun onSetGeneratedPreview(event: DocmarkCreationEvent.OnSetGeneratedPreview) {
        updateState {
            it.copy(
                previewImageBytes = event.imageBytes,
                previewImageExtension = event.extension.lowercase().removePrefix(".").ifBlank { "png" },
            )
        }
    }

    private fun onWebInitialContentLoad(event: DocmarkCreationEvent.OnWebInitialContentLoad) {
        updateState {
            it.copy(
                isLoading = false,
                error = if (event.result == WebShellLoadResult.Error) DocmarkCreationError.PreviewExtractionFailed else null,
            )
        }
    }

    private fun onChangeLabel(event: DocmarkCreationEvent.OnChangeLabel) {
        updateState { it.copy(label = event.newLabel) }
    }

    private fun onChangeDescription(event: DocmarkCreationEvent.OnChangeDescription) {
        updateState { it.copy(description = event.newDescription) }
    }

    private fun onChangeSummary(event: DocmarkCreationEvent.OnChangeSummary) {
        updateState { it.copy(summary = event.newSummary) }
    }

    private fun onSelectFolder(event: DocmarkCreationEvent.OnSelectFolder) {
        updateState {
            it.copy(
                selectedFolder = event.folder,
                uncategorizedFolderCreationRequired = false,
            )
        }
    }

    private fun onSelectTags(event: DocmarkCreationEvent.OnSelectTags) {
        updateState { it.copy(selectedTags = event.tags) }
    }

    private fun onSave(event: DocmarkCreationEvent.OnSave) {
        val state = currentState()
        var selectedFolder = state.selectedFolder ?: return

        if (!state.isInEditMode && state.documentBytes == null) {
            updateState { it.copy(error = DocmarkCreationError.NoDocument) }
            event.onErrorCallback()
            return
        }

        if (!state.isInEditMode && state.docmarkType == null) {
            updateState { it.copy(error = DocmarkCreationError.NoDocument) }
            event.onErrorCallback()
            return
        }

        launch {
            updateState { it.copy(isSaving = true, error = null) }

            try {
                if (state.uncategorizedFolderCreationRequired) {
                    selectedFolder = FolderManager.ensureUncategorizedFolderVisible()
                }

                val bookmarkId = state.editingDocmark?.id ?: IdGenerator.newId()
                val label = state.label.ifBlank { "Document" }
                if (state.editingDocmark != null) {
                    AllBookmarksManager.updateBookmarkMetadata(
                        bookmarkId = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.FILE,
                        label = label,
                        description = state.description.ifBlank { null },
                        tagIds = state.selectedTags.map { it.id },
                    )

                    val existingTagIds = state.editingDocmark.tags.map { it.id }.toSet()
                    val newTagIds = state.selectedTags.map { it.id }.toSet()
                    state.selectedTags.filter { it.id !in existingTagIds }.forEach { tag ->
                        AllBookmarksManager.addTagToBookmark(tag.id, bookmarkId)
                    }
                    state.editingDocmark.tags.filter { it.id !in newTagIds }.forEach { tag ->
                        AllBookmarksManager.removeTagFromBookmark(tag.id, bookmarkId)
                    }
                } else {
                    AllBookmarksManager.createBookmarkMetadata(
                        id = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.FILE,
                        label = label,
                        description = state.description.ifBlank { null },
                        tagIds = state.selectedTags.map { it.id },
                        previewImageBytes = state.previewImageBytes,
                        previewImageExtension = state.previewImageExtension,
                        previewIconBytes = null,
                    )
                    val docBytes = state.documentBytes ?: error("Missing document bytes")
                    val docType = state.docmarkType ?: error("Missing docmark type")
                    DocmarkFileManager.saveDocumentBytes(
                        bookmarkId = bookmarkId,
                        bytes = docBytes,
                        type = docType,
                    )
                }

                DocmarkManager.createOrUpdateDocDetails(
                    bookmarkId = bookmarkId,
                    summary = state.summary.ifBlank { null },
                    docmarkType = if (state.editingDocmark != null) null else state.docmarkType,
                    metadataTitle = state.metadataTitle,
                    metadataDescription = state.metadataDescription,
                    metadataAuthor = state.metadataAuthor,
                    metadataDate = state.metadataDate,
                    metadataIdentifier = state.metadataIdentifier,
                )

                updateState { it.copy(isSaving = false, error = null) }
                event.onSavedCallback()
            } catch (_: Exception) {
                updateState {
                    it.copy(
                        isSaving = false,
                        error = DocmarkCreationError.SaveFailed,
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
