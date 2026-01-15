package dev.subfly.yaba.ui.creation.tag

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.tag.TagCreationEvent
import dev.subfly.yabacore.state.tag.TagCreationStateMachine
import dev.subfly.yabacore.state.tag.TagCreationUIState

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