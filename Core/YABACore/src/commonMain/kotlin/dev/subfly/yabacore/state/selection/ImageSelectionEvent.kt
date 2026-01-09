package dev.subfly.yabacore.state.selection

sealed class ImageSelectionEvent {
    /**
     * Initialize image selection with available image URLs and their byte array data.
     *
     * @param imageDataMap Map of image URL to its byte array data (in-memory).
     * @param selectedImageUrl The currently selected image URL (if any).
     */
    data class OnInit(
        val imageDataMap: Map<String, ByteArray> = emptyMap(),
        val selectedImageUrl: String? = null,
    ) : ImageSelectionEvent()

    /**
     * Select an image by its URL.
     */
    data class OnSelectImage(
        val imageUrl: String,
    ) : ImageSelectionEvent()
}

