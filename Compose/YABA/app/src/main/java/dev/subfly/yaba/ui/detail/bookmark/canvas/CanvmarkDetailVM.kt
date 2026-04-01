package dev.subfly.yaba.ui.detail.bookmark.canvas

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailEvent
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailStateMachine

class CanvmarkDetailVM : ViewModel() {
    private val stateMachine = CanvmarkDetailStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: CanvmarkDetailEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
