package dev.subfly.yaba.core.deeplink

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DeepLinkManager {
    private val _pendingTarget = MutableStateFlow<DeepLinkTarget?>(null)
    val pendingTarget: StateFlow<DeepLinkTarget?> = _pendingTarget.asStateFlow()

    fun handle(target: DeepLinkTarget) {
        _pendingTarget.value = target
    }

    fun consume() {
        _pendingTarget.value = null
    }
}
