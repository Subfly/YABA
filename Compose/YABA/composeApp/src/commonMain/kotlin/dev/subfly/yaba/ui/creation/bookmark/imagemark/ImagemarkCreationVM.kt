package dev.subfly.yaba.ui.creation.bookmark.imagemark

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.creation.imagemark.ImagemarkCreationEvent
import dev.subfly.yabacore.state.creation.imagemark.ImagemarkCreationStateMachine

class ImagemarkCreationVM : ViewModel() {
    private val stateMachine = ImagemarkCreationStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: ImagemarkCreationEvent) {
        stateMachine.onEvent(event)
    }

    fun pickFromGallery() {
        stateMachine.onEvent(ImagemarkCreationEvent.OnPickFromGallery)
    }

    fun captureFromCamera() {
        stateMachine.onEvent(ImagemarkCreationEvent.OnCaptureFromCamera)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
