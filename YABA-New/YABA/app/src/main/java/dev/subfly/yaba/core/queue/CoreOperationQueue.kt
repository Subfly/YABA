package dev.subfly.yaba.core.queue

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * The queue is started from [MainActivity] before the splash screen is dismissed.
 */
object CoreOperationQueue {
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
     * Starts the queue worker. Call once during app startup (e.g. from MainActivity).
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
                } finally {
                    _queueSize.value = (_queueSize.value - 1).coerceAtLeast(0)
                    _currentEventName.value = null
                }
            }
        }
    }

    /**
     * Enqueues an operation for sequential execution.
     */
    fun queue(event: CoreEvent) {
        _queueSize.value += 1
        val result = channel.trySend(event)
        if (result.isFailure) {
            _queueSize.value = (_queueSize.value - 1).coerceAtLeast(0)
        }
    }

    fun queue(name: String, operation: suspend () -> Unit) {
        queue(CoreEvent(name, operation))
    }

    suspend fun queueAndAwait(name: String, operation: suspend () -> Unit): Result<Unit> {
        val event = CoreEvent(name, operation)
        queue(event)
        return event.await()
    }

    fun isProcessing(): Boolean = _currentEventName.value != null

    fun hasPendingEvents(): Boolean = _queueSize.value > 0
}

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

    fun complete(result: Result<Unit>) {
        completion.complete(result)
    }

    suspend fun await(): Result<Unit> = completion.await()
}
