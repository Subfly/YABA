package dev.subfly.yabasync

import kotlinx.serialization.Serializable

@Serializable
sealed class SyncEvent {
    @Serializable
    data class Handshake(val deviceId: String, val timestamp: Long) : SyncEvent()
    
    @Serializable
    data class DataSync(val data: String, val version: Long) : SyncEvent()
    
    @Serializable
    data class Acknowledge(val eventId: String, val timestamp: Long) : SyncEvent()
    
    @Serializable
    data class Error(val message: String, val code: Int) : SyncEvent()
    
    @Serializable
    data class DeleteLogExchange(val deleteLogs: List<DeleteLog>) : SyncEvent()
    
    @Serializable
    data class FullDataExchange(val syncData: SyncData) : SyncEvent()
    
    @Serializable
    data class SyncComplete(val mergedData: SyncData) : SyncEvent()
} 