package dev.subfly.yaba.ui.search

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.search.SearchEvent
import dev.subfly.yabacore.state.search.SearchStateMachine
import dev.subfly.yabacore.state.search.SearchUIState

class SearchVM : ViewModel() {
    private val stateMachine = SearchStateMachine()
    var state = mutableStateOf(SearchUIState())

    init { stateMachine.onState { newState -> state.value = newState } }

    fun onEvent(event: SearchEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
