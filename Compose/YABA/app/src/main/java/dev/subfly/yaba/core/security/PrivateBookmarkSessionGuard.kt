package dev.subfly.yaba.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory session: after a successful password entry, private bookmark actions and queries
 * may proceed until the app process backgrounds, then [lock] is called.
 */
object PrivateBookmarkSessionGuard {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    val isLocked: Boolean
        get() = _isUnlocked.value.not()

    fun unlock() {
        _isUnlocked.update { true }
    }

    fun lock() {
        _isUnlocked.update { false }
    }
}
