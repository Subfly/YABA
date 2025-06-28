package dev.subfly.yabasync

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Client implementation for device-to-device sync
 * Handles WebSocket connection to the server
 */
expect class SyncClient {
    val events: SharedFlow<SyncEvent>
    
    /**
     * Connect to a server using the provided address
     * @param serverAddress The WebSocket address (e.g., "ws://192.168.1.100:8080/sync")
     */
    suspend fun connect(serverAddress: String)
    
    /**
     * Send an event to the server
     */
    suspend fun sendEvent(event: SyncEvent)
    
    /**
     * Disconnect from the server
     */
    suspend fun disconnect()
} 