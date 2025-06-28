package dev.subfly.yabasync

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.cors.*
import io.ktor.server.plugins.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.net.InetAddress

/**
 * Server implementation for device-to-device sync
 * Handles WebSocket connections and event broadcasting
 */
expect class SyncServer(private val port: Int) {
    val events: SharedFlow<SyncEvent>
    
    /**
     * Start the server and return server information
     */
    suspend fun start(): ServerInfo
    
    /**
     * Stop the server and cleanup
     */
    fun stop()
    
    /**
     * Broadcast an event to all connected clients
     */
    suspend fun broadcastEvent(event: SyncEvent)
} 