package dev.subfly.yabasync

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object WaitingForClient : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
} 