package dev.subfly.yaba.core.state.base

/**
 * Minimal contract for state holders. Implementations expose [stateFlow] for Compose
 * (`collectAsState()` / `collectAsStateWithLifecycle()`).
 */
interface StateMachine<S, E> {
    /** Dispatch an event into the state machine. Implementations handle routing internally. */
    fun onEvent(event: E)

    /** Cancel ongoing work. Call when the machine is no longer needed. */
    fun clear()
}
