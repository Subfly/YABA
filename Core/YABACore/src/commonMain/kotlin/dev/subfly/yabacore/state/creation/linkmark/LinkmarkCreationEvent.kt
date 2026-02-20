package dev.subfly.yabacore.state.creation.linkmark

import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.unfurl.ConverterAssetInput

/**
 * Events for linkmark creation/editing state machine.
 */
sealed class LinkmarkCreationEvent {
    /**
     * Initialize the state machine.
     *
     * @param linkmarkIdString If provided, loads existing linkmark for editing.
     * @param initialUrl If provided, pre-fills the URL field (e.g., from share extension).
     * @param initialFolderId If provided, pre-selects a folder.
     * @param initialTagIds If provided, pre-selects tags.
     */
    data class OnInit(
        val linkmarkIdString: String? = null,
        val initialUrl: String? = null,
        val initialFolderId: String? = null,
        val initialTagIds: List<String>? = null,
    ) : LinkmarkCreationEvent()

    /**
     * Cycle through preview appearances in order:
     * LIST -> CARD (SMALL) -> CARD (BIG) -> GRID -> LIST...
     */
    data object OnCyclePreviewAppearance : LinkmarkCreationEvent()

    /**
     * Select a different preview image.
     *
     * @param imageUrl The URL of the selected image.
     * @param imageData Optional image data if available (from image selection screen).
     */
    data class OnSelectImage(
        val imageUrl: String,
        val imageData: ByteArray? = null,
    ) : LinkmarkCreationEvent()

    /**
     * Change the URL. This triggers debounced cleaning and re-fetching of link metadata.
     */
    data class OnChangeUrl(val newUrl: String) : LinkmarkCreationEvent()

    /**
     * Change the label/title of the linkmark.
     */
    data class OnChangeLabel(val newLabel: String) : LinkmarkCreationEvent()

    /**
     * Change the description of the linkmark.
     */
    data class OnChangeDescription(val newDescription: String) : LinkmarkCreationEvent()

    /**
     * Change the link type classification.
     */
    data class OnChangeLinkType(val linkType: LinkType) : LinkmarkCreationEvent()

    /**
     * Select the folder for this bookmark (from folder selection screen).
     */
    data class OnSelectFolder(val folder: FolderUiModel) : LinkmarkCreationEvent()

    /**
     * Set the tags for this bookmark (from tag selection screen).
     * Replaces all previously selected tags.
     */
    data class OnSelectTags(val tags: List<TagUiModel>) : LinkmarkCreationEvent()

    /**
     * Clear the label field.
     */
    data object OnClearLabel : LinkmarkCreationEvent()

    /**
     * Clear the description field.
     */
    data object OnClearDescription : LinkmarkCreationEvent()

    /**
     * Manually trigger a re-fetch of link metadata.
     */
    data object OnRefetch : LinkmarkCreationEvent()

    /**
     * Apply unfurl-detected updates (preview image/icon, videoUrl, readable content).
     *
     * This is intended for edit mode: we can fetch new remote-derived content without overwriting
     * the user's title/description automatically.
     */
    data object OnApplyContentUpdates : LinkmarkCreationEvent()

    /**
     * Converter (WebView) finished successfully with markdown + asset mappings.
     */
    data class OnConverterSucceeded(
        val markdown: String,
        val assets: List<ConverterAssetInput>,
        val title: String?,
        val author: String?,
    ) : LinkmarkCreationEvent()

    /**
     * Converter (WebView) failed.
     */
    data class OnConverterFailed(val error: Throwable) : LinkmarkCreationEvent()

    /**
     * Save the linkmark (create or update).
     */
    data class OnSave(
        val onSavedCallback: () -> Unit,
        val onErrorCallback: () -> Unit,
    ) : LinkmarkCreationEvent()
}
