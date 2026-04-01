package dev.subfly.yaba.ui.selection.icon

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.selection.icon.IconSelectionEvent
import dev.subfly.yaba.core.state.selection.icon.IconSelectionStateMachine

class IconSelectionVM : ViewModel() {
    private val stateMachine = IconSelectionStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: IconSelectionEvent) {
        stateMachine.onEvent(event)
    }

    fun getSelectedIcon(): String = stateMachine.getSelectedIcon()

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
