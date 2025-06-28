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
 * macOS-specific client implementation for device-to-device sync
 * Handles WebSocket connection to the server
 */
actual class SyncClient {
    private var session: DefaultClientWebSocketSession? = null
    private val _events = MutableSharedFlow<SyncEvent>()
    actual val events: SharedFlow<SyncEvent> = _events.asSharedFlow()
    
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient {
        install(WebSockets) {
            pingPeriod = kotlin.time.Duration.seconds(15)
            timeout = kotlin.time.Duration.seconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
    }
    
    /**
     * Connect to a server using the provided address
     * @param serverAddress The WebSocket address (e.g., "ws://192.168.1.100:8080/sync")
     */
    actual suspend fun connect(serverAddress: String) {
        session = client.webSocketSession(serverAddress)
        
        // Start listening for incoming messages
        session?.let { wsSession ->
            try {
                for (frame in wsSession.incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            try {
                                val event = json.decodeFromString<SyncEvent>(text)
                                _events.emit(event)
                            } catch (e: Exception) {
                                // Handle parsing errors
                                val errorEvent = SyncEvent.Error(
                                    message = "Failed to parse server event: ${e.message}",
                                    code = 500
                                )
                                _events.emit(errorEvent)
                            }
                        }
                        is Frame.Close -> {
                            break
                        }
                        else -> {
                            // Ignore other frame types
                        }
                    }
                }
            } catch (e: Exception) {
                throw Exception("WebSocket connection failed: ${e.message}")
            }
        }
    }
    
    /**
     * Send an event to the server
     */
    actual suspend fun sendEvent(event: SyncEvent) {
        val eventJson = json.encodeToString(event)
        session?.send(Frame.Text(eventJson))
            ?: throw IllegalStateException("Not connected to server")
    }
    
    /**
     * Disconnect from the server
     */
    actual suspend fun disconnect() {
        session?.close()
        session = null
        client.close()
    }
} 