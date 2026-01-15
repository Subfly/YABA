package dev.subfly.yaba.ui.detail.tag

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.detail.tag.TagDetailEvent
import dev.subfly.yabacore.state.detail.tag.TagDetailStateMachine
import dev.subfly.yabacore.state.detail.tag.TagDetailUIState
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class TagDetailVM() : ViewModel() {
    private val stateMachine = TagDetailStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: TagDetailEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
