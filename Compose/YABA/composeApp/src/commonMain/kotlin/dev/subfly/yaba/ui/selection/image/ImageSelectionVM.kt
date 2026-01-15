package dev.subfly.yaba.ui.selection.image

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.selection.ImageSelectionEvent
import dev.subfly.yabacore.state.selection.ImageSelectionStateMachine

class ImageSelectionVM : ViewModel() {
    private val stateMachine = ImageSelectionStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: ImageSelectionEvent) {
        stateMachine.onEvent(event)
    }

    fun getSelectedImageUrl(): String? = stateMachine.getSelectedImageUrl()

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
