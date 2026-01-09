package dev.subfly.yaba.ui.selection.image

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.selection.ImageSelectionEvent
import dev.subfly.yabacore.state.selection.ImageSelectionStateMachine
import dev.subfly.yabacore.state.selection.ImageSelectionUIState

class ImageSelectionVM : ViewModel() {
    private val stateMachine = ImageSelectionStateMachine()
    var state = mutableStateOf(ImageSelectionUIState())
        private set

    init {
        stateMachine.onState { newState -> state.value = newState }
    }

    fun onEvent(event: ImageSelectionEvent) {
        stateMachine.onEvent(event)
    }

    /**
     * Returns the currently selected image URL. Use this when returning to the bookmark creation
     * screen.
     */
    fun getSelectedImageUrl(): String? = stateMachine.getSelectedImageUrl()

    /** Returns the byte array data for the currently selected image URL. */
    fun getSelectedImageData(): ByteArray? = stateMachine.getSelectedImageData()

    /** Returns the byte array data for a given image URL. */
    fun getImageData(url: String): ByteArray? = stateMachine.getImageData(url)

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
