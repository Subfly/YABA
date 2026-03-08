package dev.subfly.yabacore.state.creation.docmark

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.filesystem.DocmarkFileManager
import dev.subfly.yabacore.filesystem.access.YabaFileAccessor
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.DocmarkManager
import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.managers.ReadableContentManager
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.state.base.BaseStateMachine
import dev.subfly.yabacore.unfurl.ReadableUnfurl
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

    override fun onEvent(event: DocmarkCreationEvent) {
        when (event) {
            is DocmarkCreationEvent.OnInit -> onInit(event)
            DocmarkCreationEvent.OnPickPdf -> onPickPdf()
            DocmarkCreationEvent.OnClearPdf -> onClearPdf()
            is DocmarkCreationEvent.OnSetGeneratedPreview -> onSetGeneratedPreview(event)
            is DocmarkCreationEvent.OnSetInternalReadableMarkdown -> onSetInternalReadableMarkdown(event)
            is DocmarkCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is DocmarkCreationEvent.OnChangeDescription -> onChangeDescription(event)
            is DocmarkCreationEvent.OnChangeSummary -> onChangeSummary(event)
            is DocmarkCreationEvent.OnSelectFolder -> onSelectFolder(event)
            is DocmarkCreationEvent.OnSelectTags -> onSelectTags(event)
            is DocmarkCreationEvent.OnSave -> onSave(event)
        }
    }

    private fun onInit(event: DocmarkCreationEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true

        launch {
            event.docmarkIdString?.let { docmarkId ->
                val existing = DocmarkManager.getDocmarkDetail(docmarkId)
                if (existing != null) {
                    updateState {
                        it.copy(
                            label = existing.label,
                            description = existing.description ?: "",
                            summary = existing.summary ?: "",
                            selectedFolder = existing.parentFolder,
                            selectedTags = existing.tags,
                            editingDocmark = existing,
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

    private fun onPickPdf() {
        if (currentState().isInEditMode) return
        launch {
            updateState { it.copy(isLoading = true, error = null) }
            try {
                val file = YabaFileAccessor.pickSingleFile(extensions = listOf("pdf")) ?: run {
                    updateState { it.copy(isLoading = false) }
                    return@launch
                }
                val bytes = withContext(Dispatchers.IO) {
                    file.readBytes()
                }
                val sourceName = file.name
                updateState {
                    it.copy(
                        pdfBytes = bytes,
                        sourceFileName = sourceName,
                        label = if (it.label.isBlank()) sourceName.substringBeforeLast('.') else it.label,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (_: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = DocmarkCreationError.PdfReadFailed,
                    )
                }
            }
        }
    }

    private fun onClearPdf() {
        if (currentState().isInEditMode) return
        updateState {
            it.copy(
                pdfBytes = null,
                sourceFileName = null,
                previewImageBytes = null,
                internalReadableMarkdown = null,
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

    private fun onSetInternalReadableMarkdown(event: DocmarkCreationEvent.OnSetInternalReadableMarkdown) {
        updateState { it.copy(internalReadableMarkdown = event.markdown) }
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

        if (!state.isInEditMode && state.pdfBytes == null) {
            updateState { it.copy(error = DocmarkCreationError.NoPdf) }
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
                val label = state.label.ifBlank { state.sourceFileName?.substringBeforeLast('.') ?: "Document" }
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
                    val pdfBytes = state.pdfBytes ?: error("Missing PDF bytes")
                    DocmarkFileManager.savePdfBytes(
                        bookmarkId = bookmarkId,
                        bytes = pdfBytes,
                    )
                }

                DocmarkManager.createOrUpdateDocDetails(
                    bookmarkId = bookmarkId,
                    summary = state.summary.ifBlank { null },
                )

                state.internalReadableMarkdown
                    ?.takeIf { it.isNotBlank() && state.isInEditMode.not() }
                    ?.let { markdown ->
                        ReadableContentManager.saveReadableContent(
                            bookmarkId = bookmarkId,
                            readable = ReadableUnfurl(
                                markdown = markdown,
                                title = label,
                                author = null,
                                assets = emptyList(),
                            ),
                        )
                    }

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
