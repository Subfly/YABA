package dev.subfly.yabacore.toast

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.state.base.StateSubscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Global toast manager used by all platforms.
 *
 * - Max 3 visible toasts at once.
 * - Extra toasts are queued and promoted when a slot becomes free.
 * - UI consumes only [visibleToasts] (or [onToasts]) and renders independently.
 */
object ToastManager {
    private const val MAX_VISIBLE_TOAST_COUNT = 3
    private const val DISMISS_ANIMATION_MILLIS = 150L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()

    private val _visibleToasts = MutableStateFlow<List<ToastItem>>(emptyList())
    val visibleToasts: StateFlow<List<ToastItem>> = _visibleToasts.asStateFlow()

    private data class PendingToast(
        val toast: ToastItem,
        val onAcceptPressed: (() -> Unit)?,
    )

    private val queuedToasts = ArrayDeque<PendingToast>()
    private val onAcceptCallbacks = mutableMapOf<ToastId, () -> Unit>()
    private val autoHideJobs = mutableMapOf<ToastId, Job>()

    fun show(
        message: PlatformToastText,
        acceptText: PlatformToastText? = null,
        iconType: ToastIconType = ToastIconType.NONE,
        duration: ToastDuration = ToastDuration.SHORT,
        onAcceptPressed: (() -> Unit)? = null,
    ): ToastId {
        val id = IdGenerator.newId()
        val toast = ToastItem(
            id = id,
            message = message,
            acceptText = acceptText,
            iconType = iconType,
            duration = duration,
            isVisible = true,
        )

        scope.launch {
            lock.withLock {
                val visible = _visibleToasts.value
                if (visible.size < MAX_VISIBLE_TOAST_COUNT) {
                    _visibleToasts.value = visible + toast
                    if (onAcceptPressed != null) onAcceptCallbacks[id] = onAcceptPressed
                    startAutoHideJobLocked(id = id, duration = duration)
                    return@withLock
                }

                queuedToasts.addLast(
                    PendingToast(
                        toast = toast,
                        onAcceptPressed = onAcceptPressed,
                    )
                )
            }
        }

        return id
    }

    fun dismiss(id: ToastId) {
        scope.launch {
            val shouldRemoveAfterAnimation = lock.withLock {
                val current = _visibleToasts.value
                val visibleIndex = current.indexOfFirst { it.id == id }

                if (visibleIndex == -1) {
                    removeQueuedToastByIdLocked(id)
                    return@withLock false
                }

                val target = current[visibleIndex]
                if (!target.isVisible) return@withLock false

                cancelAutoHideLocked(id)
                val next = current.toMutableList()
                next[visibleIndex] = target.copy(isVisible = false)
                _visibleToasts.value = next
                true
            }

            if (!shouldRemoveAfterAnimation) return@launch

            delay(DISMISS_ANIMATION_MILLIS)
            removeVisibleToastAndPromoteNext(id)
        }
    }

    fun dismissAll() {
        scope.launch {
            val hadVisibleToasts = lock.withLock {
                cancelAllAutoHideJobsLocked()
                queuedToasts.clear()
                onAcceptCallbacks.clear()

                if (_visibleToasts.value.isEmpty()) return@withLock false

                _visibleToasts.value = _visibleToasts.value.map { it.copy(isVisible = false) }
                true
            }

            if (!hadVisibleToasts) return@launch

            delay(DISMISS_ANIMATION_MILLIS)
            lock.withLock { _visibleToasts.value = emptyList() }
        }
    }

    fun accept(id: ToastId) {
        scope.launch {
            val callback = lock.withLock { onAcceptCallbacks[id] }
            runCatching { callback?.invoke() }
            dismiss(id)
        }
    }

    /**
     * Callback channel for SwiftUI and other callback-driven consumers.
     */
    fun onToasts(collector: (List<ToastItem>) -> Unit): StateSubscription {
        val job = scope.launch(Dispatchers.Main.immediate) {
            visibleToasts.collect { collector(it) }
        }
        return StateSubscription { job.cancel() }
    }

    private suspend fun removeVisibleToastAndPromoteNext(id: ToastId) {
        lock.withLock {
            val visible = _visibleToasts.value.toMutableList()
            val removed = visible.removeAll { it.id == id }
            if (!removed) return@withLock

            cancelAutoHideLocked(id)
            onAcceptCallbacks.remove(id)

            while (visible.size < MAX_VISIBLE_TOAST_COUNT && queuedToasts.isNotEmpty()) {
                val pending = queuedToasts.removeFirst()
                val promotedToast = pending.toast.copy(isVisible = true)
                visible += promotedToast
                if (pending.onAcceptPressed != null) {
                    onAcceptCallbacks[promotedToast.id] = pending.onAcceptPressed
                }
                startAutoHideJobLocked(
                    id = promotedToast.id,
                    duration = promotedToast.duration,
                )
            }

            _visibleToasts.value = visible
        }
    }

    private fun removeQueuedToastByIdLocked(id: ToastId): Boolean {
        val iterator = queuedToasts.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            if (pending.toast.id != id) continue
            iterator.remove()
            return true
        }
        return false
    }

    private fun startAutoHideJobLocked(
        id: ToastId,
        duration: ToastDuration,
    ) {
        cancelAutoHideLocked(id)
        autoHideJobs[id] = scope.launch {
            delay(duration.millis)
            dismiss(id)
        }
    }

    private fun cancelAutoHideLocked(id: ToastId) {
        autoHideJobs.remove(id)?.cancel()
    }

    private fun cancelAllAutoHideJobsLocked() {
        autoHideJobs.values.forEach { it.cancel() }
        autoHideJobs.clear()
    }
}
