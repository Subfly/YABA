package dev.subfly.yaba.ui.selection.icon

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.selection.icon.IconCategorySelectionEvent
import dev.subfly.yaba.core.state.selection.icon.IconCategorySelectionStateMachine

class IconCategorySelectionVM : ViewModel() {
    private val stateMachine = IconCategorySelectionStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: IconCategorySelectionEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
