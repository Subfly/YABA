package dev.subfly.yaba.ui.creation.notemark.mention

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.creation.notemark.NotemarkMentionCreationEvent
import dev.subfly.yabacore.state.creation.notemark.NotemarkMentionCreationStateMachine

class NotemarkMentionCreationVM : ViewModel() {
    private val stateMachine = NotemarkMentionCreationStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: NotemarkMentionCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
