package dev.subfly.yaba.ui.creation.folder

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.creation.folder.FolderCreationEvent
import dev.subfly.yabacore.state.creation.folder.FolderCreationStateMachine

class FolderCreationVM : ViewModel() {
    private val stateMachine = FolderCreationStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: FolderCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}