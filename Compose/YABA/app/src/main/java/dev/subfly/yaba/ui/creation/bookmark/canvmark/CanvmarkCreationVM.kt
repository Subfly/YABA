package dev.subfly.yaba.ui.creation.bookmark.canvmark

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.creation.canvmark.CanvmarkCreationEvent
import dev.subfly.yaba.core.state.creation.canvmark.CanvmarkCreationStateMachine

class CanvmarkCreationVM : ViewModel() {
    private val stateMachine = CanvmarkCreationStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: CanvmarkCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
