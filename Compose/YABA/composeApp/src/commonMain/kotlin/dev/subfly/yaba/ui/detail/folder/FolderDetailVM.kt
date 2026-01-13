package dev.subfly.yaba.ui.detail.folder

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.detail.folder.FolderDetailEvent
import dev.subfly.yabacore.state.detail.folder.FolderDetailStateMachine
import dev.subfly.yabacore.state.detail.folder.FolderDetailUIState
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class FolderDetailVM() : ViewModel() {
    private val stateMachine = FolderDetailStateMachine()
    var state = mutableStateOf(FolderDetailUIState())

    init { stateMachine.onState { newState -> state.value = newState } }

    fun onEvent(event: FolderDetailEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
