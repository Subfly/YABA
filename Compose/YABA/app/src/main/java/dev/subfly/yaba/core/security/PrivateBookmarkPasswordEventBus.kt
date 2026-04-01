package dev.subfly.yaba.core.security

import dev.subfly.yaba.util.PrivateBookmarkPasswordEntryResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Emits after a successful private-bookmark password entry when the caller needs a follow-up
 * action (open, edit, share, delete). Toggle-private flows are handled inside the sheet instead.
 */
object PrivateBookmarkPasswordEventBus {
    private val _events = MutableSharedFlow<PrivateBookmarkPasswordEntryResult>(extraBufferCapacity = 1)
    val events: SharedFlow<PrivateBookmarkPasswordEntryResult> = _events.asSharedFlow()

    fun emit(result: PrivateBookmarkPasswordEntryResult) {
        _events.tryEmit(result)
    }
}
