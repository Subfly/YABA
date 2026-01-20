package dev.subfly.yabacore.state.selection.image

import dev.subfly.yabacore.state.base.BaseStateMachine

class ImageSelectionStateMachine :
    BaseStateMachine<ImageSelectionUIState, ImageSelectionEvent>(
        initialState = ImageSelectionUIState()
    ) {

    private var isInitialized = false

    override fun onEvent(event: ImageSelectionEvent) {
        when (event) {
            is ImageSelectionEvent.OnInit -> onInit(event)
            is ImageSelectionEvent.OnSelectImage -> onSelectImage(event)
        }
    }

    private fun onInit(event: ImageSelectionEvent.OnInit) {
        if (isInitialized) return
        isInitialized = true

        // Use launch to ensure this runs AFTER onState's listener registration completes
        launch {
            updateState {
                it.copy(
                    imageDataMap = event.imageDataMap,
                    selectedImageUrl = event.selectedImageUrl,
                    isLoading = false,
                )
            }
        }
    }

    private fun onSelectImage(event: ImageSelectionEvent.OnSelectImage) {
        updateState { it.copy(selectedImageUrl = event.imageUrl) }
    }

    /**
     * Returns the currently selected image URL.
     */
    fun getSelectedImageUrl(): String? = currentState().selectedImageUrl

    /**
     * Returns the byte array data for the currently selected image URL.
     */
    fun getSelectedImageData(): ByteArray? {
        val selectedUrl = currentState().selectedImageUrl ?: return null
        return currentState().imageDataMap[selectedUrl]
    }

    /**
     * Returns the byte array data for a given image URL.
     */
    fun getImageData(url: String): ByteArray? = currentState().imageDataMap[url]

    override fun clear() {
        isInitialized = false
        super.clear()
    }
}

