package dev.subfly.yaba.ui.selection.bookmark

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.selection.bookmark.BookmarkSelectionEvent
import dev.subfly.yaba.core.state.selection.bookmark.BookmarkSelectionStateMachine

class BookmarkSelectionVM : ViewModel() {
    private val stateMachine = BookmarkSelectionStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: BookmarkSelectionEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
