package dev.subfly.yabasync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Orchestrates the complete sync process between two devices
 * Handles delete log exchange, data exchange, and merging
 */
class SyncOrchestrator(
    private val syncManager: SyncManager,
    private val dataProvider: SyncDataProvider
) {
    private val _syncProgress = MutableStateFlow<SyncProgress>(SyncProgress.Initializing)
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
    
    private val _mergedData = MutableStateFlow<SyncData?>(null)
    val mergedData: StateFlow<SyncData?> = _mergedData.asStateFlow()
    
    private val json = Json { ignoreUnknownKeys = true }
    private var localDeleteLogs: List<DeleteLog> = emptyList()
    private var remoteDeleteLogs: List<DeleteLog> = emptyList()
    private var localData: SyncData? = null
    private var remoteData: SyncData? = null
    
    /**
     * Start the sync process
     */
    suspend fun startSync() {
        try {
            _syncProgress.value = SyncProgress.Initializing
            
            // Subscribe to sync events
            syncManager.subscribeToEvents { event ->
                handleSyncEvent(event)
            }
            
            // Exchange delete logs first
            awaitDeleteLogExchange()
            
            // Exchange full data
            awaitDataExchange()
            
            // Merge data
            awaitDataMerging()
            
            _syncProgress.value = SyncProgress.Completed
            
        } catch (e: Exception) {
            _syncProgress.value = SyncProgress.Error(e.message ?: "Sync failed")
            throw e
        }
    }
    
    private suspend fun awaitDeleteLogExchange() {
        _syncProgress.value = SyncProgress.ExchangingDeleteLogs
        
        // Get local delete logs
        localDeleteLogs = dataProvider.getDeleteLogs()
        
        // Send local delete logs
        syncManager.sendSyncEvent(
            SyncEvent.DeleteLogExchange(localDeleteLogs)
        )
        
        // Wait for remote delete logs (handled in handleSyncEvent)
        // This is a simplified approach - in a real implementation,
        // you'd want proper request/response handling
    }
    
    private suspend fun awaitDataExchange() {
        _syncProgress.value = SyncProgress.SendingData
        
        // Get local data
        localData = dataProvider.getFullSyncData()
        
        // Send local data
        syncManager.sendSyncEvent(
            SyncEvent.FullDataExchange(localData!!)
        )
        
        // Wait for remote data (handled in handleSyncEvent)
    }
    
    private suspend fun awaitDataMerging() {
        _syncProgress.value = SyncProgress.MergingData
        
        // Merge the data
        val merged = mergeData()
        _mergedData.value = merged
        
        // Send completion event
        syncManager.sendSyncEvent(
            SyncEvent.SyncComplete(merged)
        )
    }
    
    private fun handleSyncEvent(event: SyncEvent) {
        when (event) {
            is SyncEvent.DeleteLogExchange -> {
                remoteDeleteLogs = event.deleteLogs
            }
            is SyncEvent.FullDataExchange -> {
                remoteData = event.syncData
            }
            is SyncEvent.SyncComplete -> {
                // Remote device has completed sync
                _mergedData.value = event.mergedData
            }
            is SyncEvent.Error -> {
                _syncProgress.value = SyncProgress.Error(event.message)
            }
            else -> {
                // Handle other events if needed
            }
        }
    }
    
    private fun mergeData(): SyncData {
        val local = localData ?: throw IllegalStateException("Local data not available")
        val remote = remoteData ?: throw IllegalStateException("Remote data not available")
        
        // Apply delete logs first
        val localAfterDeletes = applyDeleteLogs(local, localDeleteLogs + remoteDeleteLogs)
        val remoteAfterDeletes = applyDeleteLogs(remote, localDeleteLogs + remoteDeleteLogs)
        
        // Merge collections and bookmarks using last-write-wins
        val mergedCollections = mergeCollections(
            localAfterDeletes.collections,
            remoteAfterDeletes.collections
        )
        
        val mergedBookmarks = mergeBookmarks(
            localAfterDeletes.bookmarks,
            remoteAfterDeletes.bookmarks
        )
        
        // Combine delete logs (remove duplicates)
        val combinedDeleteLogs = (localDeleteLogs + remoteDeleteLogs)
            .distinctBy { it.logId }
            .sortedBy { it.timestamp }
        
        return SyncData(
            id = System.currentTimeMillis().toString(),
            exportedFrom = "YABASync",
            collections = mergedCollections,
            bookmarks = mergedBookmarks,
            deleteLogs = combinedDeleteLogs
        )
    }
    
    private fun applyDeleteLogs(data: SyncData, deleteLogs: List<DeleteLog>): SyncData {
        val deleteAllLog = deleteLogs.find { it.actionType == ActionType.DELETED_ALL }
        
        if (deleteAllLog != null) {
            // If there's a delete all, return empty data
            return SyncData(
                id = data.id,
                exportedFrom = data.exportedFrom,
                collections = emptyList(),
                bookmarks = emptyList(),
                deleteLogs = data.deleteLogs
            )
        }
        
        // Get IDs to delete
        val deletedIds = deleteLogs
            .filter { it.actionType == ActionType.DELETED }
            .map { it.entityId }
            .toSet()
        
        // Filter out deleted items
        val filteredCollections = data.collections.filter { 
            !deletedIds.contains(it.collectionId) 
        }
        
        val filteredBookmarks = data.bookmarks.filter { 
            !deletedIds.contains(it.bookmarkId) 
        }
        
        return data.copy(
            collections = filteredCollections,
            bookmarks = filteredBookmarks
        )
    }
    
    private fun mergeCollections(
        local: List<SyncCollection>,
        remote: List<SyncCollection>
    ): List<SyncCollection> {
        val merged = mutableMapOf<String, SyncCollection>()
        
        // Add local collections
        local.forEach { collection ->
            merged[collection.collectionId] = collection
        }
        
        // Merge with remote collections (last-write-wins)
        remote.forEach { remoteCollection ->
            val localCollection = merged[remoteCollection.collectionId]
            
            if (localCollection == null) {
                // New collection from remote
                merged[remoteCollection.collectionId] = remoteCollection
            } else {
                // Conflict - use last-write-wins based on editedAt
                val localTime = localCollection.editedAt.toLongOrNull() ?: 0L
                val remoteTime = remoteCollection.editedAt.toLongOrNull() ?: 0L
                
                if (remoteTime > localTime) {
                    merged[remoteCollection.collectionId] = remoteCollection
                }
            }
        }
        
        return merged.values.toList()
    }
    
    private fun mergeBookmarks(
        local: List<SyncBookmark>,
        remote: List<SyncBookmark>
    ): List<SyncBookmark> {
        val merged = mutableMapOf<String, SyncBookmark>()
        
        // Add local bookmarks
        local.forEach { bookmark ->
            val id = bookmark.bookmarkId ?: bookmark.link
            merged[id] = bookmark
        }
        
        // Merge with remote bookmarks (last-write-wins)
        remote.forEach { remoteBookmark ->
            val id = remoteBookmark.bookmarkId ?: remoteBookmark.link
            val localBookmark = merged[id]
            
            if (localBookmark == null) {
                // New bookmark from remote
                merged[id] = remoteBookmark
            } else {
                // Conflict - use last-write-wins based on editedAt
                val localTime = localBookmark.editedAt?.toLongOrNull() ?: 0L
                val remoteTime = remoteBookmark.editedAt?.toLongOrNull() ?: 0L
                
                if (remoteTime > localTime) {
                    merged[id] = remoteBookmark
                }
            }
        }
        
        return merged.values.toList()
    }
}

/**
 * Interface for providing sync data to the orchestrator
 * Implementations should provide data from the local database
 */
interface SyncDataProvider {
    /**
     * Get all delete logs from the local database
     */
    suspend fun getDeleteLogs(): List<DeleteLog>
    
    /**
     * Get full sync data including collections, bookmarks, and image data
     */
    suspend fun getFullSyncData(): SyncData
    
    /**
     * Apply merged data to the local database
     * This is called after successful sync
     */
    suspend fun applyMergedData(data: SyncData)
} 