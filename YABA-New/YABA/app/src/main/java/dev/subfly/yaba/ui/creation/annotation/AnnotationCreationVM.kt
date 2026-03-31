package dev.subfly.yaba.ui.creation.annotation

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.creation.annotation.AnnotationCreationEvent
import dev.subfly.yaba.core.state.creation.annotation.AnnotationCreationStateMachine

class AnnotationCreationVM : ViewModel() {
    private val stateMachine = AnnotationCreationStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: AnnotationCreationEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
