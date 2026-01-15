package dev.subfly.yaba.ui.home

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.home.HomeEvent
import dev.subfly.yabacore.state.home.HomeStateMachine

class HomeVM : ViewModel() {
    private val stateMachine = HomeStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: HomeEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}