package dev.subfly.yaba.ui.creation.tag

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.creation.tag.TagCreationEvent
import dev.subfly.yabacore.state.creation.tag.TagCreationStateMachine

class TagCreationVM: ViewModel() {
    private val stateMachine = TagCreationStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: TagCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}