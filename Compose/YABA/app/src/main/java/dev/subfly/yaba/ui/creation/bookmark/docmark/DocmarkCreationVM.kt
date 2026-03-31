package dev.subfly.yaba.ui.creation.bookmark.docmark

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.creation.docmark.DocmarkCreationEvent
import dev.subfly.yaba.core.state.creation.docmark.DocmarkCreationStateMachine

class DocmarkCreationVM : ViewModel() {
    private val stateMachine = DocmarkCreationStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: DocmarkCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
