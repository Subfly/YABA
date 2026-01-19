package dev.subfly.yabacore.queue

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

/**
 * A FIFO operation queue that executes persistence operations sequentially.
 *
 * All content CRUD operations (create, update, delete) are enqueued here and executed
 * in order. This ensures:
 * - Deterministic ordering (call-order preserved via synchronous trySend)
 * - Operations outlive UI/state-machine lifecycle
 * - No race conditions between concurrent operations
 *
 * The queue is started from [CoreRuntime.initialize] and lives until the app is terminated.
 */
object CoreOperationQueue {
    private val logger = Logger
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channel = Channel<CoreEvent>(Channel.UNLIMITED)

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    private val _currentEventName = MutableStateFlow<String?>(null)
    val currentEventName: StateFlow<String?> = _currentEventName.asStateFlow()

    private val _lastError = MutableStateFlow<Throwable?>(null)
    val lastError: StateFlow<Throwable?> = _lastError.asStateFlow()

    @Volatile
    private var isStarted = false

    /**
     * Starts the queue worker. Call once from [CoreRuntime.initialize].
     * Subsequent calls are no-ops.
     */
    fun start() {
        if (isStarted) return
        isStarted = true

        scope.launch {
            for (event in channel) {
                _currentEventName.value = event.name
                try {
                    event.execute()
                    event.complete(Result.success(Unit))
                    _lastError.value = null
                } catch (e: Throwable) {
                    event.complete(Result.failure(e))
                    _lastError.value = e
                    // Log but don't crash - continue processing queue
                    logger.e("CoreOperationQueue: Error executing ${event.name}: ${e.message}")
                } finally {
                    _queueSize.value = (_queueSize.value - 1).coerceAtLeast(0)
                    _currentEventName.value = null
                }
            }
        }
    }

    /**
     * Enqueues an operation for sequential execution.
     *
     * Uses [Channel.trySend] to preserve call-order determinism. Since the channel
     * is UNLIMITED, trySend will always succeed immediately, ensuring that concurrent
     * callers cannot reorder their sends.
     *
     * @param event The event to execute. Must be a suspend function that performs
     *              the actual persistence work (filesystem, DB, CRDT).
     */
    fun queue(event: CoreEvent) {
        _queueSize.value += 1
        val result = channel.trySend(event)
        if (result.isFailure) {
            // Should not happen with UNLIMITED channel, but handle gracefully
            _queueSize.value = (_queueSize.value - 1).coerceAtLeast(0)
            logger.e("CoreOperationQueue: Failed to enqueue ${event.name}")
        }
    }

    /**
     * Convenience method to enqueue a simple operation with a name.
     */
    fun queue(name: String, operation: suspend () -> Unit) {
        queue(CoreEvent(name, operation))
    }

    /**
     * Enqueues an operation and suspends until it completes.
     *
     * This is useful for startup operations that must complete before continuing,
     * such as ensuring system folders/tags exist.
     *
     * @param name A human-readable name for debugging/visibility.
     * @param operation The suspend function that performs the actual work.
     * @return Result indicating success or failure of the operation.
     */
    suspend fun queueAndAwait(name: String, operation: suspend () -> Unit): Result<Unit> {
        val event = CoreEvent(name, operation)
        queue(event)
        return event.await()
    }

    /**
     * Returns whether the queue is currently processing any events.
     */
    fun isProcessing(): Boolean = _currentEventName.value != null

    /**
     * Returns whether there are pending events in the queue.
     */
    fun hasPendingEvents(): Boolean = _queueSize.value > 0
}

/**
 * Represents a single operation to be executed by the queue.
 *
 * @param name A human-readable name for debugging/visibility.
 * @param entityId Optional entity ID this operation relates to.
 * @param execute The suspend function that performs the actual work.
 */
class CoreEvent(
    val name: String,
    val entityId: String? = null,
    private val operation: suspend () -> Unit,
) {
    private val completion = CompletableDeferred<Result<Unit>>()

    constructor(name: String, operation: suspend () -> Unit) : this(name, null, operation)

    suspend fun execute() {
        operation()
    }

    /**
     * Completes this event with the given result.
     * Called by the queue worker after execution.
     */
    fun complete(result: Result<Unit>) {
        completion.complete(result)
    }

    /**
     * Awaits completion of this event.
     * @return Result indicating success or failure.
     */
    suspend fun await(): Result<Unit> = completion.await()
}
