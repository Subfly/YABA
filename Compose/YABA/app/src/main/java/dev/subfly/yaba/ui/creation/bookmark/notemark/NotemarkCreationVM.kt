package dev.subfly.yaba.ui.creation.bookmark.notemark

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.creation.notemark.NotemarkCreationEvent
import dev.subfly.yaba.core.state.creation.notemark.NotemarkCreationStateMachine

class NotemarkCreationVM : ViewModel() {
    private val stateMachine = NotemarkCreationStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: NotemarkCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
