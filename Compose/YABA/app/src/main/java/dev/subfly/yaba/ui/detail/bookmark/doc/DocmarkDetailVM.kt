package dev.subfly.yaba.ui.detail.bookmark.doc

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.detail.docmark.DocmarkDetailEvent
import dev.subfly.yaba.core.state.detail.docmark.DocmarkDetailStateMachine

class DocmarkDetailVM : ViewModel() {
    private val stateMachine = DocmarkDetailStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: DocmarkDetailEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
