package dev.subfly.yabacore.state.selection

import androidx.compose.runtime.Immutable

@Immutable
data class ImageSelectionUIState(
    /**
     * Currently selected image URL.
     */
    val selectedImageUrl: String? = null,

    /**
     * Map of image URL to its byte array data (in-memory).
     * Used to display images without re-downloading.
     */
    val imageDataMap: Map<String, ByteArray> = emptyMap(),

    /**
     * Indicates whether the state is being initialized.
     */
    val isLoading: Boolean = true,
) {
    /**
     * Get the byte array for a given image URL.
     */
    fun getImageData(url: String): ByteArray? = imageDataMap[url]
}

