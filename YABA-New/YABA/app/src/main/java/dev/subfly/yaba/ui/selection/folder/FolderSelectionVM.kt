package dev.subfly.yaba.ui.selection.folder

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.selection.folder.FolderSelectionEvent
import dev.subfly.yaba.core.state.selection.folder.FolderSelectionStateMachine

class FolderSelectionVM : ViewModel() {
    private val stateMachine = FolderSelectionStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: FolderSelectionEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
