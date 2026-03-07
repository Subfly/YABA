package dev.subfly.yaba.ui.creation.highlight

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.creation.highlight.HighlightCreationEvent
import dev.subfly.yabacore.state.creation.highlight.HighlightCreationStateMachine

class HighlightCreationVM : ViewModel() {
    private val stateMachine = HighlightCreationStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: HighlightCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
