package dev.subfly.yaba.ui.search

import androidx.lifecycle.ViewModel
import dev.subfly.yaba.core.state.search.SearchEvent
import dev.subfly.yaba.core.state.search.SearchStateMachine
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class SearchVM : ViewModel() {
    private val stateMachine = SearchStateMachine()
    var state = stateMachine.stateFlow

    fun onEvent(event: SearchEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
