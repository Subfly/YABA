package dev.subfly.yaba.core.state.creation.imagemark

import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel

/**
 * Events for imagemark creation/editing state machine.
 */
sealed class ImagemarkCreationEvent {
    /**
     * Initialize the state machine.
     *
     * @param imagemarkIdString If provided, loads existing imagemark for editing.
     * @param initialFolderId If provided, pre-selects a folder.
     * @param initialTagIds If provided, pre-selects tags.
     */
    data class OnInit(
        val imagemarkIdString: String? = null,
        val initialFolderId: String? = null,
        val initialTagIds: List<String>? = null,
    ) : ImagemarkCreationEvent()

    /**
     * Cycle through preview appearances: LIST -> CARD (SMALL) -> CARD (BIG) -> GRID -> LIST...
     */
    data object OnCyclePreviewAppearance : ImagemarkCreationEvent()

    /**
     * User requested to pick an image from gallery.
     * The state machine will open the picker, read bytes, and update state.
     */
    data object OnPickFromGallery : ImagemarkCreationEvent()

    /**
     * Image was shared from another app (e.g. share extension).
     * Pre-populates the creation form with the given image.
     */
    data class OnImageFromShare(
        val bytes: ByteArray,
        val extension: String,
    ) : ImagemarkCreationEvent() {
        override fun equals(other: Any?): Boolean =
            other is OnImageFromShare && bytes.contentEquals(other.bytes) && extension == other.extension

        override fun hashCode(): Int = bytes.contentHashCode() * 31 + extension.hashCode()
    }

    /**
     * User requested to capture a photo from camera.
     * The state machine will open the camera, read bytes, and update state.
     */
    data object OnCaptureFromCamera : ImagemarkCreationEvent()

    /**
     * Clear the selected image (user wants to pick a different one).
     */
    data object OnClearImage : ImagemarkCreationEvent()

    /**
     * Change the label/title of the imagemark.
     */
    data class OnChangeLabel(val newLabel: String) : ImagemarkCreationEvent()

    /**
     * Change the description of the imagemark.
     */
    data class OnChangeDescription(val newDescription: String) : ImagemarkCreationEvent()

    /**
     * Change the summary of the imagemark.
     */
    data class OnChangeSummary(val newSummary: String) : ImagemarkCreationEvent()

    /**
     * Select the folder for this bookmark (from folder selection screen).
     */
    data class OnSelectFolder(val folder: FolderUiModel) : ImagemarkCreationEvent()

    /**
     * Set the tags for this bookmark (from tag selection screen).
     */
    data class OnSelectTags(val tags: List<TagUiModel>) : ImagemarkCreationEvent()

    /**
     * Save the imagemark (create or update).
     */
    data class OnSave(
        val onSavedCallback: () -> Unit,
        val onErrorCallback: () -> Unit,
    ) : ImagemarkCreationEvent()

    data object OnTogglePinned : ImagemarkCreationEvent()
}
