package dev.subfly.yaba.ui.selection.folder

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.selection.FolderSelectionEvent
import dev.subfly.yabacore.state.selection.FolderSelectionStateMachine

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
