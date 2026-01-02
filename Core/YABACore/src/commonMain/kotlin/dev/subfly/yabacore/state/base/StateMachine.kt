package dev.subfly.yabacore.state.base

/**
 * Minimal contract for cross-platform state holders.
 *
 * Usage outline:
 * - Create the machine (e.g., via `remember { ... }` in Compose or property in SwiftUI).
 * - Register with [onState] to get Main-thread callbacks and keep the returned subscription.
 * - Fire [onEvent] to request work.
 * - Call [clear] and cancel the subscription when the machine is no longer needed.
 *
 * No Flow exposure is required on iOS; callbacks are the only public state channel.
 */
interface StateMachine<S, E> {
    /** Dispatch an event into the state machine. Implementations handle routing internally. */
    fun onEvent(event: E)

    /**
     * Register a callback to receive state updates. The callback is invoked on the Main dispatcher.
     * Returns a [StateSubscription] that can be cancelled to stop receiving updates (e.g., on
     * SwiftUI `onDisappear` or Compose `DisposableEffect`).
     */
    fun onState(collector: (S) -> Unit): StateSubscription

    /** Cancel ongoing work and drop callbacks. Call when the machine is no longer needed. */
    fun clear()
}

/** Lightweight handle to dispose a previously registered state callback. */
class StateSubscription
internal constructor(
        private val onCancel: () -> Unit,
) {
    fun cancel() = onCancel()
}
