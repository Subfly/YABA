package dev.subfly.yaba.core.security

import dev.subfly.yaba.core.toast.ToastIconType
import dev.subfly.yaba.core.toast.ToastManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory session: after a successful password entry, private bookmark actions and queries
 * may proceed until the app process backgrounds, then [lock] is called.
 */
object PrivateBookmarkSessionGuard {
    // TODO: LOCALIZATION — toast when the private-bookmark session becomes locked
    private const val SESSION_LOCKED_TOAST_MESSAGE =
        "Private bookmarks are locked. Enter your PIN to view them again."

    // TODO: LOCALIZATION — toast when the private-bookmark session becomes unlocked
    private const val SESSION_UNLOCKED_TOAST_MESSAGE =
        "Private bookmarks are unlocked for this session. Lock anytime from the home bar."

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    val isLocked: Boolean
        get() = _isUnlocked.value.not()

    fun unlock() {
        val wasLocked = !_isUnlocked.value
        _isUnlocked.update { true }
        if (wasLocked) {
            ToastManager.show(
                message = SESSION_UNLOCKED_TOAST_MESSAGE,
                iconType = ToastIconType.HINT,
            )
        }
    }

    fun lock() {
        val wasUnlocked = _isUnlocked.value
        _isUnlocked.update { false }
        if (wasUnlocked) {
            ToastManager.show(
                message = SESSION_LOCKED_TOAST_MESSAGE,
                iconType = ToastIconType.HINT,
            )
        }
    }
}
