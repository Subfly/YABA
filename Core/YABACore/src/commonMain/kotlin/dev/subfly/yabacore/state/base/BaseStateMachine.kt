package dev.subfly.yabacore.state.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared base implementation to centralize coroutine handling and state publishing.
 *
 * - Work runs on a private `SupervisorJob + Dispatchers.IO` scope.
 * - State callbacks are dispatched on `Dispatchers.Main` to make UI updates safe for SwiftUI and
 * Compose.
 * - No coroutine scope is exposed publicly; subclasses get protected helpers only.
 */
abstract class BaseStateMachine<S, E>(
    initialState: S,
) : StateMachine<S, E> {
    private val job: Job = SupervisorJob()
    private val scope: CoroutineScope = CoroutineScope(job + Dispatchers.IO)
    private val _stateFlow = MutableStateFlow(initialState)
    private val listeners = mutableSetOf<(S) -> Unit>()
    private val listenersLock = Mutex()

    /**
     * Expose the state flow for Compose consumption via `collectAsState()`. For SwiftUI, prefer
     * [onState] callback-based approach.
     */
    val stateFlow: StateFlow<S>
        get() = _stateFlow.asStateFlow()

    /** Run suspended work on the internal IO scope. */
    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        scope.launch(block = block)

    /** Convenience accessor for the latest state. Only subclasses can reach it. */
    protected fun currentState(): S = _stateFlow.value

    /**
     * Update the state and notify listeners on Main. Reducer runs on the caller thread; be mindful
     * to avoid heavy work in the reducer itself.
     */
    protected fun updateState(reducer: (S) -> S) {
        val newState = reducer(_stateFlow.value)
        _stateFlow.value = newState
        publishState(newState)
    }

    override fun onState(collector: (S) -> Unit): StateSubscription {
        launch {
            listenersLock.withLock { listeners.add(collector) }
            publishState(_stateFlow.value, collector)
        }
        return StateSubscription {
            launch { listenersLock.withLock { listeners.remove(collector) } }
        }
    }

    private fun publishState(
        state: S,
        target: ((S) -> Unit)? = null,
    ): Job =
        scope.launch(Dispatchers.Main.immediate) {
            if (target != null) {
                target(state)
                return@launch
            }
            val callbacks = listenersLock.withLock { listeners.toList() }
            callbacks.forEach { it(state) }
        }

    override fun clear() {
        runBlocking { listenersLock.withLock { listeners.clear() } }
        job.cancel()
    }
}
