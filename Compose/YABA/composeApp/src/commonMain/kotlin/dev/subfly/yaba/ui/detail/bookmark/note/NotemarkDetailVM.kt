package dev.subfly.yaba.ui.detail.bookmark.note

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailEvent
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailStateMachine

class NotemarkDetailVM : ViewModel() {
    private val stateMachine = NotemarkDetailStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: NotemarkDetailEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
