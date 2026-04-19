@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.subfly.yaba.core.state.creation.linkmark

import dev.subfly.yaba.core.common.IdGenerator
import dev.subfly.yaba.core.filesystem.LinkmarkFileManager
import dev.subfly.yaba.core.managers.AllBookmarksManager
import dev.subfly.yaba.core.managers.FolderManager
import dev.subfly.yaba.core.managers.LinkmarkManager
import dev.subfly.yaba.core.managers.ReadableContentManager
import dev.subfly.yaba.core.managers.TagManager
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.state.base.BaseStateMachine
import dev.subfly.yaba.core.toast.ToastIconType
import dev.subfly.yaba.core.toast.ToastManager
import dev.subfly.yaba.core.unfurl.ConverterResultProcessor
import dev.subfly.yaba.core.webview.normalizeBridgeOptionalString
import dev.subfly.yaba.core.unfurl.UnfurlError
import dev.subfly.yaba.core.unfurl.Unfurler
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
    private var toastMessages: LinkmarkCreationToastMessages? = null

    override fun onEvent(event: LinkmarkCreationEvent) {
        when (event) {
            is LinkmarkCreationEvent.OnInit -> onInit(event)
            LinkmarkCreationEvent.OnCyclePreviewAppearance -> onCyclePreviewAppearance()
            is LinkmarkCreationEvent.OnChangeUrl -> onChangeUrl(event)
            is LinkmarkCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is LinkmarkCreationEvent.OnChangeDescription -> onChangeDescription(event)
            is LinkmarkCreationEvent.OnSelectFolder -> onSelectFolder(event)
            is LinkmarkCreationEvent.OnSelectTags -> onSelectTags(event)
            is LinkmarkCreationEvent.OnSave -> onSave(event)
            LinkmarkCreationEvent.OnTogglePinned -> onTogglePinned()
            LinkmarkCreationEvent.OnClearLabel -> onClearLabel()
            LinkmarkCreationEvent.OnClearDescription -> onClearDescription()
            LinkmarkCreationEvent.OnApplyFromMetadata -> onApplyFromMetadata()
            LinkmarkCreationEvent.OnRefetch -> onRefetch()
            is LinkmarkCreationEvent.OnConverterSucceeded -> onConverterSucceeded(event)
            is LinkmarkCreationEvent.OnConverterFailed -> onConverterFailed(event)
        }
    }

    private fun onInit(event: LinkmarkCreationEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true
        toastMessages = event.toastMessages

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
                            metadataTitle = existing.metadataTitle,
                            metadataDescription = existing.metadataDescription,
                            metadataAuthor = existing.metadataAuthor,
                            metadataDate = existing.metadataDate,
                            videoUrl = existing.videoUrl,
                            audioUrl = existing.audioUrl,
                            selectedFolder = existing.parentFolder,
                            selectedTags = existing.tags,
                            editingLinkmark = existing,
                            lastFetchedUrl = existing.url,
                            isPinned = existing.isPinned,
                        )
                    }

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
    ) {
        val state = currentState()

        if (urlString.isBlank() || (!force && urlString == state.lastFetchedUrl)) {
            return
        }

        updateState {
            it.copy(
                isLoading = true,
                error = null,
            )
        }

        try {
            val preview = Unfurler.unfurl(urlString)

            if (preview != null) {
                val rawHtml = preview.rawHtml
                val needsConverter = rawHtml.isNullOrBlank().not()

                updateState {
                    it.copy(
                        cleanedUrl = preview.url,
                        host = extractDomain(preview.url),
                        converterHtml = if (needsConverter) rawHtml else null,
                        converterBaseUrl = if (needsConverter) preview.url else null,
                        converterError = null,
                        lastFetchedUrl = urlString,
                        isLoading = needsConverter,
                        error = null,
                    )
                }
                showUnfurlToast(
                    message = toastMessages?.unfurlSuccess,
                    iconType = ToastIconType.SUCCESS,
                )
            } else {
                val fallback = urlString.trim()
                updateState {
                    it.copy(
                        cleanedUrl = fallback,
                        host = extractDomain(fallback),
                        lastFetchedUrl = urlString,
                        isLoading = false,
                        error = LinkmarkCreationError.UnableToUnfurl,
                    )
                }
                showUnfurlToast(
                    message = toastMessages?.unableToUnfurl,
                    iconType = ToastIconType.ERROR,
                )
            }
        } catch (e: UnfurlError.CannotCreateUrl) {
            updateState {
                it.copy(
                    isLoading = false,
                    error = LinkmarkCreationError.InvalidUrl(e.raw),
                )
            }
            showUnfurlToast(
                message = toastMessages?.invalidUrl,
                iconType = ToastIconType.ERROR,
            )
        } catch (_: UnfurlError) {
            val fallback = urlString.trim()
            updateState {
                it.copy(
                    cleanedUrl = fallback,
                    host = extractDomain(fallback),
                    lastFetchedUrl = urlString,
                    isLoading = false,
                    error = LinkmarkCreationError.UnableToUnfurl,
                )
            }
            showUnfurlToast(
                message = toastMessages?.unableToUnfurl,
                iconType = ToastIconType.ERROR,
            )
        } catch (_: Exception) {
            val fallback = urlString.trim()
            updateState {
                it.copy(
                    cleanedUrl = fallback,
                    host = extractDomain(fallback),
                    lastFetchedUrl = urlString,
                    isLoading = false,
                    error = LinkmarkCreationError.FetchFailed,
                )
            }
            showUnfurlToast(
                message = toastMessages?.genericUnfurlError,
                iconType = ToastIconType.ERROR,
            )
        }
    }

    private fun extractDomain(url: String): String {
        val withoutProtocol = url.substringAfter("://", url)
        val candidate = withoutProtocol.substringBefore("/")
        return candidate.substringBefore("?").substringBefore("#")
    }

    private fun showUnfurlToast(
        message: String?,
        iconType: ToastIconType,
    ) {
        val toastMessage = message ?: return
        val acceptLabel = toastMessages?.acceptLabel

        ToastManager.show(
            message = toastMessage,
            acceptText = acceptLabel,
            iconType = iconType,
        )
    }

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

    private fun onChangeUrl(event: LinkmarkCreationEvent.OnChangeUrl) {
        if (currentState().isInEditMode) return
        updateState { it.copy(url = event.newUrl) }
        launch { urlDebounceFlow.emit(event.newUrl) }
    }

    private fun onChangeLabel(event: LinkmarkCreationEvent.OnChangeLabel) {
        updateState { it.copy(label = event.newLabel) }
    }

    private fun onChangeDescription(event: LinkmarkCreationEvent.OnChangeDescription) {
        updateState { it.copy(description = event.newDescription) }
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

    private fun onApplyFromMetadata() {
        val state = currentState()
        val metaTitle = state.metadataTitle?.trim().orEmpty()
        val metaDesc = state.metadataDescription?.trim().orEmpty()
        updateState {
            it.copy(
                label = metaTitle.ifEmpty { it.label },
                description = metaDesc.ifEmpty { it.description },
            )
        }
    }

    private fun onTogglePinned() {
        updateState { it.copy(isPinned = !it.isPinned) }
    }

    private fun onRefetch() {
        if (currentState().isInEditMode) return
        val currentUrl = currentState().url
        if (currentUrl.isNotBlank()) {
            updateState { it.copy(lastFetchedUrl = "") }
            launch {
                fetchLinkData(
                    urlString = currentUrl,
                    force = true,
                )
            }
        }
    }

    private fun onConverterSucceeded(event: LinkmarkCreationEvent.OnConverterSucceeded) {
        if (currentState().isInEditMode) return
        launch {
            val meta = event.linkMetadata
            val imageBytes =
                Unfurler.downloadPreviewImageBytes(meta.image.normalizeBridgeOptionalString())
            val logoBytes = Unfurler.downloadPreviewImageBytes(meta.logo.normalizeBridgeOptionalString())
            val readable = ConverterResultProcessor.process(
                documentJson = event.documentJson,
                assets = event.assets,
            )
            updateState {
                it.copy(
                    cleanedUrl = meta.cleanedUrl,
                    host = extractDomain(meta.cleanedUrl),
                    metadataTitle = meta.title?.normalizeBridgeOptionalString(),
                    metadataDescription = meta.description?.normalizeBridgeOptionalString(),
                    metadataAuthor = meta.author?.normalizeBridgeOptionalString(),
                    metadataDate = meta.date?.normalizeBridgeOptionalString(),
                    videoUrl = meta.video?.normalizeBridgeOptionalString(),
                    audioUrl = meta.audio?.normalizeBridgeOptionalString(),
                    imageData = imageBytes ?: it.imageData,
                    iconData = logoBytes ?: it.iconData,
                    readable = readable,
                    converterHtml = null,
                    converterBaseUrl = null,
                    converterError = null,
                    isLoading = false,
                )
            }
        }
    }

    private fun onConverterFailed(event: LinkmarkCreationEvent.OnConverterFailed) {
        updateState {
            it.copy(
                converterHtml = null,
                converterBaseUrl = null,
                converterError = event.error.message ?: "Conversion failed",
                isLoading = false,
            )
        }
    }

    private fun onSave(event: LinkmarkCreationEvent.OnSave) {
        val state = currentState()
        val title = state.label.trim()
        if (title.isEmpty()) return
        var selectedFolder = state.selectedFolder ?: return

        launch {
            updateState { it.copy(isSaving = true) }

            try {
                if (state.uncategorizedFolderCreationRequired) {
                    selectedFolder = FolderManager.ensureUncategorizedFolderVisible()
                }

                val bookmarkId: String
                if (state.editingLinkmark != null) {
                    bookmarkId = state.editingLinkmark.id

                    AllBookmarksManager.updateBookmarkMetadata(
                        bookmarkId = bookmarkId,
                        folderId = selectedFolder.id,
                        kind = BookmarkKind.LINK,
                        label = title,
                        description = state.description.ifBlank { null },
                        isPinned = state.isPinned,
                        previewImageBytes = null,
                        previewImageExtension = null,
                        previewIconBytes = null,
                    )

                    LinkmarkManager.createOrUpdateLinkDetails(
                        bookmarkId = bookmarkId,
                        url = state.editingLinkmark.url,
                        domain = state.editingLinkmark.domain,
                        videoUrl = state.editingLinkmark.videoUrl,
                        audioUrl = state.editingLinkmark.audioUrl,
                        metadataTitle = state.editingLinkmark.metadataTitle,
                        metadataDescription = state.editingLinkmark.metadataDescription,
                        metadataAuthor = state.editingLinkmark.metadataAuthor,
                        metadataDate = state.editingLinkmark.metadataDate,
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
                        label = title,
                        description = state.description.ifBlank { null },
                        isPinned = state.isPinned,
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
                        videoUrl = state.videoUrl,
                        audioUrl = state.audioUrl,
                        metadataTitle = state.metadataTitle,
                        metadataDescription = state.metadataDescription,
                        metadataAuthor = state.metadataAuthor,
                        metadataDate = state.metadataDate,
                    )
                }

                // Save readable content if available (immutable versioning)
                state.readable?.let { readable ->
                    ReadableContentManager.saveReadableContent(bookmarkId, readable)
                }

                updateState { it.copy(isSaving = false, error = null) }
                event.onSavedCallback()
            } catch (_: Exception) {
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
        toastMessages = null
        urlDebounceJob?.cancel()
        urlDebounceJob = null
        super.clear()
    }
}
