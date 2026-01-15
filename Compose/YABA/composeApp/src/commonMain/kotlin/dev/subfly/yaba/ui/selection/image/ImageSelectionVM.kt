package dev.subfly.yaba.ui.selection.image

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.selection.ImageSelectionEvent
import dev.subfly.yabacore.state.selection.ImageSelectionStateMachine
import dev.subfly.yabacore.state.selection.ImageSelectionUIState

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
