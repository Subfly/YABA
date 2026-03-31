package dev.subfly.yaba.core.state.creation.linkmark

import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.webview.WebConverterAsset
import dev.subfly.yaba.core.webview.WebLinkMetadata

/**
 * Events for linkmark creation/editing state machine.
 */
data class LinkmarkCreationToastMessages(
    val unfurlSuccess: String,
    val invalidUrl: String,
    val unableToUnfurl: String,
    val genericUnfurlError: String,
    val acceptLabel: String,
)

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
     * @param toastMessages If provided, used for toast emission from state machine flows.
     */
    data class OnInit(
        val linkmarkIdString: String? = null,
        val initialUrl: String? = null,
        val initialFolderId: String? = null,
        val initialTagIds: List<String>? = null,
        val toastMessages: LinkmarkCreationToastMessages? = null,
    ) : LinkmarkCreationEvent()

    /**
     * Cycle through preview appearances in order:
     * LIST -> CARD (SMALL) -> CARD (BIG) -> GRID -> LIST...
     */
    data object OnCyclePreviewAppearance : LinkmarkCreationEvent()

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
     * Copy extracted metadata into the user-editable title and description fields.
     * Only non-blank metadata fields are applied; others keep their current values.
     */
    data object OnApplyFromMetadata : LinkmarkCreationEvent()

    /**
     * Manually trigger a re-fetch of link metadata.
     */
    data object OnRefetch : LinkmarkCreationEvent()

    /**
     * Converter (WebView) finished successfully with rich-text document JSON + asset mappings.
     */
    data class OnConverterSucceeded(
        val documentJson: String,
        val assets: List<WebConverterAsset>,
        val linkMetadata: WebLinkMetadata,
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
