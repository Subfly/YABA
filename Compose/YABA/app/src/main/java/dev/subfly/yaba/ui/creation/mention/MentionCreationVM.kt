package dev.subfly.yaba.ui.creation.mention

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.creation.mention.MentionCreationEvent
import dev.subfly.yaba.core.state.creation.mention.MentionCreationStateMachine

class MentionCreationVM : ViewModel() {
    private val stateMachine = MentionCreationStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: MentionCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
