package dev.subfly.yaba.ui.selection

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.selection.FolderSelectionEvent
import dev.subfly.yabacore.state.selection.FolderSelectionStateMachine
import dev.subfly.yabacore.state.selection.FolderSelectionUIState

class FolderSelectionVM : ViewModel() {
    private val stateMachine = FolderSelectionStateMachine()
    var state = mutableStateOf(FolderSelectionUIState())
        private set

    init {
        stateMachine.onState { newState ->
            state.value = newState
        }
    }

    fun onEvent(event: FolderSelectionEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
