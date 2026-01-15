package dev.subfly.yaba.ui.creation.bookmark.linkmark

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationEvent
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationStateMachine

class LinkmarkCreationVM : ViewModel() {
    private val stateMachine = LinkmarkCreationStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: LinkmarkCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}

