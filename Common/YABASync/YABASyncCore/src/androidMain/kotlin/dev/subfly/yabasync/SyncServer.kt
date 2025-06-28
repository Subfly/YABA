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
import java.net.NetworkInterface
import java.util.*

/**
 * Android-specific server implementation for device-to-device sync
 * Handles WebSocket connections and event broadcasting
 */
actual class SyncServer(private val port: Int) {
    private var server: ApplicationEngine? = null
    private val _events = MutableSharedFlow<SyncEvent>()
    actual val events: SharedFlow<SyncEvent> = _events.asSharedFlow()
    
    private val connectedClients = mutableSetOf<DefaultWebSocketSession>()
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Start the server and return server information
     */
    actual suspend fun start(): ServerInfo {
        val ipAddress = getLocalIpAddress() ?: "127.0.0.1"
        
        server = embeddedServer(Netty, port = port) {
            install(Logging) {
                level = org.slf4j.event.Level.INFO
            }
            
            install(CORS) {
                anyHost()
            }
            
            install(WebSockets) {
                pingPeriod = kotlin.time.Duration.seconds(15)
                timeout = kotlin.time.Duration.seconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            
            routing {
                webSocket("/sync") {
                    handleWebSocketConnection()
                }
            }
        }
        
        server?.start(wait = false)
        
        return ServerInfo(
            ipAddress = ipAddress,
            port = port,
            fullAddress = "ws://$ipAddress:$port/sync"
        )
    }
    
    /**
     * Stop the server and cleanup
     */
    actual fun stop() {
        connectedClients.forEach { it.close() }
        connectedClients.clear()
        server?.stop(1000, 2000)
        server = null
    }
    
    /**
     * Broadcast an event to all connected clients
     */
    actual suspend fun broadcastEvent(event: SyncEvent) {
        val eventJson = json.encodeToString(event)
        val clientsToRemove = mutableSetOf<DefaultWebSocketSession>()
        
        connectedClients.forEach { session ->
            try {
                session.send(Frame.Text(eventJson))
            } catch (e: Exception) {
                clientsToRemove.add(session)
            }
        }
        
        connectedClients.removeAll(clientsToRemove)
    }
    
    private suspend fun DefaultWebSocketServerSession.handleWebSocketConnection() {
        connectedClients.add(this)
        
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            val event = json.decodeFromString<SyncEvent>(text)
                            _events.emit(event)
                            
                            // Send acknowledgment
                            val ack = SyncEvent.Acknowledge(
                                eventId = System.currentTimeMillis().toString(),
                                timestamp = System.currentTimeMillis()
                            )
                            send(Frame.Text(json.encodeToString(ack)))
                        } catch (e: Exception) {
                            val errorEvent = SyncEvent.Error(
                                message = "Failed to parse event: ${e.message}",
                                code = 400
                            )
                            send(Frame.Text(json.encodeToString(errorEvent)))
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
            // Handle connection errors
        } finally {
            connectedClients.remove(this)
        }
    }
    
    private fun getLocalIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses
                
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress.hostAddress.indexOf(':') < 0) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // Handle exception
        }
        return null
    }
} 