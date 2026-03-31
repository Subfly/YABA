package dev.subfly.yaba.core.state.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared base implementation: coroutine scope on IO, state in a [StateFlow] for Compose.
 */
abstract class BaseStateMachine<S, E>(
    initialState: S,
) : StateMachine<S, E> {
    private val job: Job = SupervisorJob()
    private val scope: CoroutineScope = CoroutineScope(job + Dispatchers.IO)
    private val _stateFlow = MutableStateFlow(initialState)

    val stateFlow: StateFlow<S>
        get() = _stateFlow.asStateFlow()

    /** Run suspended work on the internal IO scope. */
    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        scope.launch(block = block)

    protected fun currentState(): S = _stateFlow.value

    /**
     * Update state. Reducer runs on the caller thread; avoid heavy work in the reducer.
     */
    protected fun updateState(reducer: (S) -> S) {
        _stateFlow.value = reducer(_stateFlow.value)
    }

    override fun clear() {
        job.cancel()
    }
}
