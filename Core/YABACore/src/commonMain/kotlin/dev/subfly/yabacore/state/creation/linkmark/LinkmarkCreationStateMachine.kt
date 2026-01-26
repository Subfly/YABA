@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.subfly.yabacore.state.creation.linkmark

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.managers.AllBookmarksManager
import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.managers.LinkmarkManager
import dev.subfly.yabacore.managers.ReadableContentManager
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.state.base.BaseStateMachine
import dev.subfly.yabacore.unfurl.LinkCleaner
import dev.subfly.yabacore.unfurl.UnfurlError
import dev.subfly.yabacore.unfurl.Unfurler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.milliseconds

private const val DEBOUNCE_DELAY_MS = 750L

@OptIn(FlowPreview::class)
class LinkmarkCreationStateMachine :
    BaseStateMachine<LinkmarkCreationUIState, LinkmarkCreationEvent>(
        initialState = LinkmarkCreationUIState()
    ) {
    private var isInitialized = false
    private val preferencesStore
        get() = SettingsStores.userPreferences

    // Debounce flow for URL changes
    private val urlDebounceFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private var urlDebounceJob: Job? = null

    override fun onEvent(event: LinkmarkCreationEvent) {
        when (event) {
            is LinkmarkCreationEvent.OnInit -> onInit(event)
            LinkmarkCreationEvent.OnCyclePreviewAppearance -> onCyclePreviewAppearance()
            is LinkmarkCreationEvent.OnSelectImage -> onSelectImage(event)
            is LinkmarkCreationEvent.OnChangeUrl -> onChangeUrl(event)
            is LinkmarkCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is LinkmarkCreationEvent.OnChangeDescription -> onChangeDescription(event)
            is LinkmarkCreationEvent.OnChangeLinkType -> onChangeLinkType(event)
            is LinkmarkCreationEvent.OnSelectFolder -> onSelectFolder(event)
            is LinkmarkCreationEvent.OnSelectTags -> onSelectTags(event)
            is LinkmarkCreationEvent.OnSave -> onSave(event)
            LinkmarkCreationEvent.OnClearLabel -> onClearLabel()
            LinkmarkCreationEvent.OnClearDescription -> onClearDescription()
            LinkmarkCreationEvent.OnRefetch -> onRefetch()
            LinkmarkCreationEvent.OnApplyContentUpdates -> onApplyContentUpdates()
        }
    }

    private fun onInit(event: LinkmarkCreationEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true

        // Start URL debounce listener
        startUrlDebounceListener()

        launch {
            // Load preferences for initial appearance
            val preferences = preferencesStore.get()
            updateState {
                it.copy(
                    bookmarkAppearance = preferences.preferredBookmarkAppearance,
                    cardImageSizing = preferences.preferredCardImageSizing,
                )
            }

            // If editing existing linkmark
            event.linkmarkIdString?.let { linkmarkId ->
                val existing = LinkmarkManager.getLinkmarkDetail(linkmarkId)
                if (existing != null) {
                    updateState {
                        it.copy(
                            url = existing.url,
                            cleanedUrl = existing.url,
                            host = existing.domain,
                            label = existing.label,
                            description = existing.description ?: "",
                            iconUrl = null,
                            imageUrl = null,
                            videoUrl = existing.videoUrl,
                            selectedLinkType = existing.linkType,
                            selectedFolder = existing.parentFolder,
                            selectedTags = existing.tags,
                            editingLinkmark = existing,
                            lastFetchedUrl = existing.url,
                        )
                    }

                    // Use the new helper methods that don't expose FileKit
                    val savedImageBytes = LinkmarkFileManager.readLinkImageBytes(linkmarkId)
                    val savedIconBytes = LinkmarkFileManager.readDomainIconBytes(linkmarkId)

                    if (savedImageBytes != null || savedIconBytes != null) {
                        updateState {
                            it.copy(
                                imageData = savedImageBytes,
                                iconData = savedIconBytes,
                            )
                        }
                    }

                    // Always run unfurler on init to populate selectable images and readable HTML.
                    // In edit mode we DO NOT overwrite user-editable fields
                    // (label/description/url/etc).
                    fetchLinkData(
                        urlString = existing.url,
                        force = true,
                        previewOnly = true,
                    )
                }
            }

            // Pre-fill initial URL if provided (e.g., from share extension)
            if (event.initialUrl != null && event.linkmarkIdString == null) {
                updateState { it.copy(url = event.initialUrl) }
                urlDebounceFlow.emit(event.initialUrl)
            }

            // Pre-select folder if provided
            event.initialFolderId?.let { folderId ->
                val folder = FolderManager.getFolder(folderId)
                if (folder != null) {
                    updateState { it.copy(selectedFolder = folder) }
                }
            }

            // Pre-select tags if provided
            event.initialTagIds?.let { tagIds ->
                val tags = tagIds.mapNotNull { tagId -> TagManager.getTag(tagId) }
                if (tags.isNotEmpty()) {
                    updateState { it.copy(selectedTags = tags) }
                }
            }

            // Ensure a folder is selected (use uncategorized if none)
            if (currentState().selectedFolder == null) {
                // Use in-memory model immediately; actual persistence + visibility restoration happens on save.
                // This ensures: if Uncategorized is currently hidden, we unhide it via UPDATE before using it.
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

    private fun startUrlDebounceListener() {
        urlDebounceJob?.cancel()
        urlDebounceJob = launch {
            urlDebounceFlow
                .debounce(DEBOUNCE_DELAY_MS.milliseconds)
                .distinctUntilChanged()
                .collect { urlToFetch -> fetchLinkData(urlToFetch) }
        }
    }

    private suspend fun fetchLinkData(
        urlString: String,
        force: Boolean = false,
        previewOnly: Boolean = false,
    ) {
        val state = currentState()

        // Skip if empty or same as last fetched
        if (urlString.isBlank() || (force.not() && urlString == state.lastFetchedUrl)) {
            return
        }

        updateState {
            if (previewOnly) {
                // Editing should feel instant: don't flip the UI into a loading/shimmer state
                // while we background-refresh selectable images / update candidates.
                it.copy(error = null)
            } else {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }
        }

        try {
            val preview = Unfurler.unfurl(urlString)

            if (preview != null) {
                val currentLabel = currentState().label
                val currentDescription = currentState().description

                updateState {
                    if (previewOnly) {
                        val imageUpdate = computeByteArrayUpdate(it.imageData, preview.imageData)
                        val iconUpdate = computeByteArrayUpdate(it.iconData, preview.iconData)
                        val videoDiff = preview.videoUrl != it.videoUrl
                        val readableDiff = preview.readable != it.readable
                        val hasUpdates =
                            (imageUpdate != null) ||
                                    (iconUpdate != null) ||
                                    videoDiff ||
                                    readableDiff
                        it.copy(
                            // Only enrich the selectable image list; everything else becomes a
                            // pending update.
                            selectableImages = preview.imageOptions,
                            hasContentUpdates = hasUpdates,
                            updateImageData = imageUpdate,
                            updateIconData = iconUpdate,
                            shouldUpdateVideoUrl = videoDiff,
                            updateVideoUrl = if (videoDiff) preview.videoUrl else null,
                            shouldUpdateReadable = readableDiff,
                            updateReadable = if (readableDiff) preview.readable else null,
                            lastFetchedUrl = urlString,
                            isLoading = false,
                            error = null,
                        )
                    } else {
                        it.copy(
                            cleanedUrl = preview.url,
                            host = preview.host ?: "",
                            // Only update label/description if they're empty
                            label = currentLabel.ifBlank { preview.title ?: "" },
                            description =
                                currentDescription.ifBlank { preview.description ?: "" },
                            iconUrl = preview.iconUrl,
                            imageUrl = preview.imageUrl,
                            videoUrl = preview.videoUrl,
                            iconData = preview.iconData,
                            imageData = preview.imageData,
                            selectableImages = preview.imageOptions,
                            readable = preview.readable,
                            // Clear any pending updates if we're not in preview-only mode.
                            hasContentUpdates = false,
                            updateImageData = null,
                            updateIconData = null,
                            shouldUpdateVideoUrl = false,
                            updateVideoUrl = null,
                            shouldUpdateReadable = false,
                            updateReadable = null,
                            lastFetchedUrl = urlString,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
            } else {
                updateState {
                    if (previewOnly) {
                        it.copy(
                            lastFetchedUrl = urlString,
                            isLoading = false,
                            error = null,
                        )
                    } else {
                        // Clean URL even if unfurl failed
                        val cleaned = LinkCleaner.clean(urlString)
                        it.copy(
                            cleanedUrl = cleaned,
                            lastFetchedUrl = urlString,
                            isLoading = false,
                            error = LinkmarkCreationError.UnableToUnfurl,
                        )
                    }
                }
            }
        } catch (e: UnfurlError.CannotCreateUrl) {
            updateState {
                it.copy(
                    isLoading = false,
                    error = if (previewOnly) null else LinkmarkCreationError.InvalidUrl(e.raw),
                )
            }
        } catch (e: UnfurlError) {
            updateState {
                if (previewOnly) {
                    it.copy(
                        lastFetchedUrl = urlString,
                        isLoading = false,
                        error = null,
                    )
                } else {
                    val cleaned = LinkCleaner.clean(urlString)
                    it.copy(
                        cleanedUrl = cleaned,
                        lastFetchedUrl = urlString,
                        isLoading = false,
                        error = LinkmarkCreationError.UnableToUnfurl,
                    )
                }
            }
        } catch (e: Exception) {
            updateState {
                if (previewOnly) {
                    it.copy(
                        lastFetchedUrl = urlString,
                        isLoading = false,
                        error = null,
                    )
                } else {
                    val cleaned = LinkCleaner.clean(urlString)
                    it.copy(
                        cleanedUrl = cleaned,
                        lastFetchedUrl = urlString,
                        isLoading = false,
                        error = LinkmarkCreationError.FetchFailed,
                    )
                }
            }
        }
    }

    /**
     * Cycles through preview appearances in order: LIST -> CARD (SMALL) -> CARD (BIG) -> GRID ->
     * LIST...
     */
    private fun onCyclePreviewAppearance() {
        val state = currentState()

        val (nextAppearance, nextSizing) =
            when (state.bookmarkAppearance) {
                BookmarkAppearance.LIST -> BookmarkAppearance.CARD to CardImageSizing.SMALL
                BookmarkAppearance.CARD -> {
                    when (state.cardImageSizing) {
                        CardImageSizing.SMALL -> BookmarkAppearance.CARD to CardImageSizing.BIG
                        CardImageSizing.BIG -> BookmarkAppearance.GRID to state.cardImageSizing
                    }
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

    private fun onSelectImage(event: LinkmarkCreationEvent.OnSelectImage) {
        val state = currentState()
        // If imageData is not provided, try to look it up from selectableImages
        val imageData = event.imageData ?: event.imageUrl.let { url -> state.selectableImages[url] }

        updateState {
            it.copy(
                imageUrl = event.imageUrl,
                imageData = imageData,
            )
        }
    }

    private fun onChangeUrl(event: LinkmarkCreationEvent.OnChangeUrl) {
        updateState { it.copy(url = event.newUrl) }
        // Emit to debounce flow
        launch { urlDebounceFlow.emit(event.newUrl) }
    }

    private fun onChangeLabel(event: LinkmarkCreationEvent.OnChangeLabel) {
        updateState { it.copy(label = event.newLabel) }
    }

    private fun onChangeDescription(event: LinkmarkCreationEvent.OnChangeDescription) {
        updateState { it.copy(description = event.newDescription) }
    }

    private fun onChangeLinkType(event: LinkmarkCreationEvent.OnChangeLinkType) {
        updateState { it.copy(selectedLinkType = event.linkType) }
    }

    private fun onSelectFolder(event: LinkmarkCreationEvent.OnSelectFolder) {
        updateState {
            it.copy(selectedFolder = event.folder, uncategorizedFolderCreationRequired = false)
        }
    }

    private fun onSelectTags(event: LinkmarkCreationEvent.OnSelectTags) {
        updateState { it.copy(selectedTags = event.tags) }
    }

    private fun onClearLabel() {
        updateState { it.copy(label = "") }
    }

    private fun onClearDescription() {
        updateState { it.copy(description = "") }
    }

    private fun onRefetch() {
        val currentUrl = currentState().url
        if (currentUrl.isNotBlank()) {
            // Reset lastFetchedUrl to force re-fetch
            updateState { it.copy(lastFetchedUrl = "") }
            launch {
                val state = currentState()
                fetchLinkData(
                    urlString = currentUrl,
                    force = true,
                    previewOnly = state.isInEditMode,
                )
            }
        }
    }

    private fun onApplyContentUpdates() {
        val state = currentState()
        if (!state.hasContentUpdates) return

        updateState {
            it.copy(
                imageData = it.updateImageData ?: it.imageData,
                iconData = it.updateIconData ?: it.iconData,
                videoUrl = if (it.shouldUpdateVideoUrl) it.updateVideoUrl else it.videoUrl,
                readable = if (it.shouldUpdateReadable) it.updateReadable else it.readable,
                hasContentUpdates = false,
                updateImageData = null,
                updateIconData = null,
                shouldUpdateVideoUrl = false,
                updateVideoUrl = null,
                shouldUpdateReadable = false,
                updateReadable = null,
            )
        }
    }

    private fun computeByteArrayUpdate(current: ByteArray?, candidate: ByteArray?): ByteArray? {
        if (candidate == null) return null
        if (current == null) return candidate
        return if (current.contentEquals(candidate)) null else candidate
    }

    private fun onSave(event: LinkmarkCreationEvent.OnSave) {
        val state = currentState()
        var selectedFolder = state.selectedFolder ?: return

        launch {
            updateState { it.copy(isSaving = true) }

            try {
                // If uncategorized folder needs to be created, persist it now
                if (state.uncategorizedFolderCreationRequired) {
                    selectedFolder = FolderManager.ensureUncategorizedFolderVisible()
                }

                val bookmarkId: String
                if (state.editingLinkmark != null) {
                    bookmarkId = state.editingLinkmark.id

                    // Update base bookmark metadata first (list/grid source of truth)
                    AllBookmarksManager.updateBookmarkMetadata(
                        bookmarkId = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.LINK,
                        label = state.label.ifBlank { state.cleanedUrl },
                        description = state.description.ifBlank { null },
                        previewImageBytes = state.imageData,
                        previewImageExtension = "jpeg",
                        previewIconBytes = state.iconData,
                    )

                    // Then upsert link-specific details (detail screen extras)
                    LinkmarkManager.createOrUpdateLinkDetails(
                        bookmarkId = bookmarkId,
                        url = state.cleanedUrl,
                        domain = state.host,
                        linkType = state.selectedLinkType,
                        videoUrl = state.videoUrl,
                    )

                    // Handle tag changes
                    val existingTagIds = state.editingLinkmark.tags.map { it.id }.toSet()
                    val newTagIds = state.selectedTags.map { it.id }.toSet()

                    // Add new tags
                    state.selectedTags.filter { it.id !in existingTagIds }.forEach { tag ->
                        AllBookmarksManager.addTagToBookmark(tag.id, bookmarkId)
                    }

                    // Remove old tags
                    state.editingLinkmark.tags.filter { it.id !in newTagIds }.forEach { tag ->
                        AllBookmarksManager.removeTagFromBookmark(tag.id, bookmarkId)
                    }
                } else {
                    // Create new base bookmark metadata first (list/grid source of truth)
                    bookmarkId = IdGenerator.newId()
                    AllBookmarksManager.createBookmarkMetadata(
                        id = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.LINK,
                        label = state.label.ifBlank { state.cleanedUrl },
                        description = state.description.ifBlank { null },
                        tagIds = state.selectedTags.map { it.id },
                        previewImageBytes = state.imageData,
                        previewImageExtension = "jpeg",
                        previewIconBytes = state.iconData,
                    )

                    // Then create link-specific details (detail screen extras)
                    LinkmarkManager.createOrUpdateLinkDetails(
                        bookmarkId = bookmarkId,
                        url = state.cleanedUrl,
                        domain = state.host,
                        linkType = state.selectedLinkType,
                        videoUrl = state.videoUrl,
                    )
                }

                // Save readable content if available (immutable versioning)
                state.readable?.let { readable ->
                    ReadableContentManager.saveReadableContent(bookmarkId, readable)
                }

                updateState { it.copy(isSaving = false, error = null) }
                event.onSavedCallback()
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isSaving = false,
                        error = LinkmarkCreationError.SaveFailed,
                    )
                }
                event.onErrorCallback()
            }
        }
    }

    override fun clear() {
        isInitialized = false
        urlDebounceJob?.cancel()
        urlDebounceJob = null
        super.clear()
    }
}
