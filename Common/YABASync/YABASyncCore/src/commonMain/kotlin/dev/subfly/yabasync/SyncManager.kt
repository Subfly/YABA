package dev.subfly.yabasync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main sync manager that handles device-to-device communication
 * Supports both client and server modes
 */
class SyncManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
    val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()
    
    private var syncServer: SyncServer? = null
    private var syncClient: SyncClient? = null
    
    /**
     * Start the server mode and return the local IP address for QR generation
     * @return ServerInfo containing the IP address and port
     */
    suspend fun startServer(port: Int = DEFAULT_PORT): ServerInfo {
        stopAll()
        
        syncServer = SyncServer(port)
        val serverInfo = syncServer!!.start()
        _serverInfo.value = serverInfo
        _connectionState.value = ConnectionState.WaitingForClient
        
        return serverInfo
    }
    
    /**
     * Connect to a server using the scanned QR code data
     * @param serverAddress The server address from QR code
     */
    suspend fun connectToServer(serverAddress: String) {
        stopAll()
        
        syncClient = SyncClient()
        _connectionState.value = ConnectionState.Connecting
        
        try {
            syncClient!!.connect(serverAddress)
            _connectionState.value = ConnectionState.Connected
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            throw e
        }
    }
    
    /**
     * Stop all connections and cleanup
     */
    fun stopAll() {
        syncServer?.stop()
        syncClient?.disconnect()
        syncServer = null
        syncClient = null
        _connectionState.value = ConnectionState.Disconnected
        _serverInfo.value = null
    }
    
    /**
     * Send a sync event to the connected peer
     */
    suspend fun sendSyncEvent(event: SyncEvent) {
        when (_connectionState.value) {
            is ConnectionState.Connected -> {
                syncClient?.sendEvent(event) ?: syncServer?.broadcastEvent(event)
            }
            else -> throw IllegalStateException("Not connected to any peer")
        }
    }
    
    /**
     * Subscribe to sync events from the connected peer
     */
    fun subscribeToEvents(onEvent: (SyncEvent) -> Unit) {
        scope.launch {
            syncClient?.events?.collect { onEvent(it) }
            syncServer?.events?.collect { onEvent(it) }
        }
    }
    
    companion object {
        const val DEFAULT_PORT = 7484
    }
} 