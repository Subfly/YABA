package dev.subfly.yaba.ui.detail.bookmark.link

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailStateMachine
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class LinkmarkDetailVM : ViewModel() {
    private val stateMachine = LinkmarkDetailStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: LinkmarkDetailEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
