package dev.subfly.yaba.ui.creation.folder

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.folder.FolderCreationEvent
import dev.subfly.yabacore.state.folder.FolderCreationStateMachine
import dev.subfly.yabacore.state.folder.FolderCreationUIState

class FolderCreationVM : ViewModel() {
    private val stateMachine = FolderCreationStateMachine()
    var state = mutableStateOf(FolderCreationUIState())

    init {
        stateMachine.onState { newState ->
            state.value = newState
        }
    }

    fun onEvent(event: FolderCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}