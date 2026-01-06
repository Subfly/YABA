@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.subfly.yabacore.state.linkmark

import dev.subfly.yabacore.managers.FolderManager
import dev.subfly.yabacore.managers.LinkmarkManager
import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.state.base.BaseStateMachine
import dev.subfly.yabacore.unfurl.LinkCleaner
import dev.subfly.yabacore.unfurl.UnfurlError
import dev.subfly.yabacore.unfurl.Unfurler
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

private const val DEBOUNCE_DELAY_MS = 750L

@OptIn(ExperimentalUuidApi::class, FlowPreview::class)
class LinkmarkCreationStateMachine :
        BaseStateMachine<LinkmarkCreationUIState, LinkmarkCreationEvent>(
                initialState = LinkmarkCreationUIState()
        ) {
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
            LinkmarkCreationEvent.OnClearLabel -> onClearLabel()
            LinkmarkCreationEvent.OnClearDescription -> onClearDescription()
            LinkmarkCreationEvent.OnRefetch -> onRefetch()
            LinkmarkCreationEvent.OnSave -> onSave()
        }
    }

    private fun onInit(event: LinkmarkCreationEvent.OnInit) {
        // Start URL debounce listener
        startUrlDebounceListener()

        launch {
            // Load preferences for initial appearance
            val preferences = preferencesStore.get()
            updateState {
                it.copy(
                        contentAppearance = preferences.preferredContentAppearance,
                        cardImageSizing = preferences.preferredCardImageSizing,
                )
            }

            // If editing existing linkmark
            event.linkmarkIdString?.let { idString ->
                val linkmarkId = Uuid.parse(idString)
                val existing = LinkmarkManager.getLinkmarkDetail(linkmarkId)
                if (existing != null) {
                    updateState {
                        it.copy(
                                url = existing.url,
                                cleanedUrl = existing.url,
                                host = existing.domain,
                                label = existing.label,
                                description = existing.description ?: "",
                                iconUrl = existing.previewIconUrl,
                                imageUrl = existing.previewImageUrl,
                                videoUrl = existing.videoUrl,
                                selectedLinkType = existing.linkType,
                                selectedFolder = existing.parentFolder,
                                selectedTags = existing.tags,
                                editingLinkmark = existing,
                                lastFetchedUrl = existing.url,
                        )
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
                val folder = FolderManager.getFolder(Uuid.parse(folderId))
                if (folder != null) {
                    updateState { it.copy(selectedFolder = folder) }
                }
            }

            // Pre-select tags if provided
            event.initialTagIds?.let { tagIds ->
                val tags = tagIds.mapNotNull { tagId -> TagManager.getTag(Uuid.parse(tagId)) }
                if (tags.isNotEmpty()) {
                    updateState { it.copy(selectedTags = tags) }
                }
            }

            // Ensure a folder is selected (use uncategorized if none)
            if (currentState().selectedFolder == null) {
                val uncategorizedFolder = FolderManager.ensureUncategorizedFolder()
                updateState {
                    it.copy(
                            selectedFolder = uncategorizedFolder,
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

    private suspend fun fetchLinkData(urlString: String) {
        val state = currentState()

        // Skip if empty or same as last fetched
        if (urlString.isBlank() || urlString == state.lastFetchedUrl) {
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
                val currentLabel = currentState().label
                val currentDescription = currentState().description

                updateState {
                    it.copy(
                            cleanedUrl = preview.url,
                            host = preview.host ?: "",
                            // Only update label/description if they're empty
                            label = currentLabel.ifBlank { preview.title ?: "" },
                            description = currentDescription.ifBlank { preview.description ?: "" },
                            iconUrl = preview.iconUrl,
                            imageUrl = preview.imageUrl,
                            videoUrl = preview.videoUrl,
                            iconData = preview.iconData,
                            imageData = preview.imageData,
                            readableHtml = preview.readableHtml,
                            lastFetchedUrl = urlString,
                            isLoading = false,
                            error = null,
                    )
                }
            } else {
                // Clean URL even if unfurl failed
                val cleaned = LinkCleaner.clean(urlString)
                updateState {
                    it.copy(
                            cleanedUrl = cleaned,
                            lastFetchedUrl = urlString,
                            isLoading = false,
                            error = LinkmarkCreationError.UnableToUnfurl,
                    )
                }
            }
        } catch (e: UnfurlError.CannotCreateUrl) {
            updateState {
                it.copy(
                        isLoading = false,
                        error = LinkmarkCreationError.InvalidUrl(e.raw),
                )
            }
        } catch (e: UnfurlError) {
            val cleaned = LinkCleaner.clean(urlString)
            updateState {
                it.copy(
                        cleanedUrl = cleaned,
                        lastFetchedUrl = urlString,
                        isLoading = false,
                        error = LinkmarkCreationError.UnableToUnfurl,
                )
            }
        } catch (e: Exception) {
            val cleaned = LinkCleaner.clean(urlString)
            updateState {
                it.copy(
                        cleanedUrl = cleaned,
                        lastFetchedUrl = urlString,
                        isLoading = false,
                        error = LinkmarkCreationError.FetchFailed,
                )
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
                when (state.contentAppearance) {
                    ContentAppearance.LIST -> ContentAppearance.CARD to CardImageSizing.SMALL
                    ContentAppearance.CARD -> {
                        when (state.cardImageSizing) {
                            CardImageSizing.SMALL -> ContentAppearance.CARD to CardImageSizing.BIG
                            CardImageSizing.BIG -> ContentAppearance.GRID to state.cardImageSizing
                        }
                    }
                    ContentAppearance.GRID -> ContentAppearance.LIST to state.cardImageSizing
                }

        updateState {
            it.copy(
                    contentAppearance = nextAppearance,
                    cardImageSizing = nextSizing,
            )
        }
    }

    private fun onSelectImage(event: LinkmarkCreationEvent.OnSelectImage) {
        updateState {
            it.copy(
                    imageUrl = event.imageUrl,
                    imageData = event.imageData,
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
        updateState { it.copy(selectedFolder = event.folder) }
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
            launch { fetchLinkData(currentUrl) }
        }
    }

    private fun onSave() {
        val state = currentState()
        val selectedFolder = state.selectedFolder ?: return

        launch {
            updateState { it.copy(isLoading = true) }

            try {
                if (state.editingLinkmark != null) {
                    // Update existing linkmark
                    val updated =
                            state.editingLinkmark.copy(
                                    folderId = selectedFolder.id,
                                    label = state.label.ifBlank { state.cleanedUrl },
                                    description = state.description.ifBlank { null },
                                    url = state.cleanedUrl,
                                    domain = state.host,
                                    linkType = state.selectedLinkType,
                                    previewImageUrl = state.imageUrl,
                                    previewIconUrl = state.iconUrl,
                                    videoUrl = state.videoUrl,
                            )
                    LinkmarkManager.updateLinkmark(updated)

                    // Save image data if available
                    state.imageData?.let { data -> LinkmarkManager.saveLinkImage(updated.id, data) }
                    state.iconData?.let { data -> LinkmarkManager.saveDomainIcon(updated.id, data) }

                    // Handle tag changes
                    val existingTagIds = state.editingLinkmark.tags.map { it.id }.toSet()
                    val newTagIds = state.selectedTags.map { it.id }.toSet()

                    // Add new tags
                    state.selectedTags.filter { it.id !in existingTagIds }.forEach { tag ->
                        TagManager.addTagToBookmark(tag, updated.id)
                    }

                    // Remove old tags
                    state.editingLinkmark.tags.filter { it.id !in newTagIds }.forEach { tag ->
                        TagManager.removeTagFromBookmark(tag, updated.id)
                    }
                } else {
                    // Create new linkmark
                    val now = Clock.System.now()
                    val newLinkmark =
                            LinkmarkUiModel(
                                    id = Uuid.random(),
                                    folderId = selectedFolder.id,
                                    kind = BookmarkKind.LINK,
                                    label = state.label.ifBlank { state.cleanedUrl },
                                    description = state.description.ifBlank { null },
                                    url = state.cleanedUrl,
                                    domain = state.host,
                                    linkType = state.selectedLinkType,
                                    previewImageUrl = state.imageUrl,
                                    previewIconUrl = state.iconUrl,
                                    videoUrl = state.videoUrl,
                                    createdAt = now,
                                    editedAt = now,
                                    parentFolder = selectedFolder,
                                    tags = state.selectedTags,
                            )
                    val created = LinkmarkManager.createLinkmark(newLinkmark)

                    // Save image data if available
                    state.imageData?.let { data -> LinkmarkManager.saveLinkImage(created.id, data) }
                    state.iconData?.let { data -> LinkmarkManager.saveDomainIcon(created.id, data) }

                    // Add tags
                    state.selectedTags.forEach { tag ->
                        TagManager.addTagToBookmark(tag, created.id)
                    }
                }

                updateState { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                            isLoading = false,
                            error = LinkmarkCreationError.SaveFailed,
                    )
                }
            }
        }
    }

    override fun clear() {
        urlDebounceJob?.cancel()
        urlDebounceJob = null
        super.clear()
    }
}
