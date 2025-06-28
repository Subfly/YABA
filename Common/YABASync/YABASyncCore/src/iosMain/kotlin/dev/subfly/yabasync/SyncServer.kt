package dev.subfly.yabasync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * iOS-specific server implementation
 * Note: Server functionality is not supported on iOS due to platform limitations
 */
actual class SyncServer(private val port: Int) {
    private val _events = MutableSharedFlow<SyncEvent>()
    actual val events: SharedFlow<SyncEvent> = _events.asSharedFlow()
    
    /**
     * Start the server and return server information
     * @throws UnsupportedOperationException Server functionality not supported on iOS
     */
    actual suspend fun start(): ServerInfo {
        throw UnsupportedOperationException("Server functionality is not supported on iOS")
    }
    
    /**
     * Stop the server and cleanup
     */
    actual fun stop() {
        // No-op for iOS
    }
    
    /**
     * Broadcast an event to all connected clients
     * @throws UnsupportedOperationException Server functionality not supported on iOS
     */
    actual suspend fun broadcastEvent(event: SyncEvent) {
        throw UnsupportedOperationException("Server functionality is not supported on iOS")
    }
} 