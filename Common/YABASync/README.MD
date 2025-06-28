# YABASync

A Kotlin Multiplatform library for device-to-device synchronization using local network communication.

## Overview

YABASync enables direct synchronization between devices on the same local network without requiring cloud services. It uses WebSocket communication over local network connections, with QR code-based device discovery and comprehensive data synchronization.

## Features

- **Local Network Communication**: Direct device-to-device sync over local network
- **QR Code Discovery**: Scan QR codes to connect devices
- **Event-Based Communication**: Real-time event streaming between devices
- **Cross-Platform Support**: Works on Android, iOS, macOS, and JVM
- **No Cloud Dependencies**: Completely offline and privacy-focused
- **Automatic IP Discovery**: Automatically finds local IP addresses for server mode
- **Full Data Synchronization**: Complete sync of bookmarks, collections, and image data
- **Delete Log Management**: Efficient handling of deletions and bulk operations
- **Conflict Resolution**: Last-write-wins strategy for data conflicts
- **Progress Tracking**: Real-time sync progress updates for UI feedback

## Architecture

### Components

1. **SyncManager**: Main entry point for sync operations
2. **SyncServer**: Handles server-side WebSocket connections (JVM/Android/macOS only)
3. **SyncClient**: Handles client-side WebSocket connections
4. **SyncOrchestrator**: Orchestrates the complete sync process
5. **QRUtils**: Utilities for QR code data generation and parsing
6. **ImageUtils**: Platform-specific Base64 encoding/decoding for image data

### Sync Flow

1. **Connection**: Server starts → Client scans QR → WebSocket connection established
2. **Delete Log Exchange**: Both devices exchange delete logs to understand what's been deleted
3. **Data Exchange**: Both devices send complete data (collections, bookmarks, images)
4. **Merging**: Data is merged using last-write-wins strategy
5. **Completion**: Merged data is returned for database updates

## Usage

### Basic Setup

```kotlin
val syncManager = SyncManager()
val dataProvider = YourSyncDataProvider() // Implement SyncDataProvider
val orchestrator = SyncOrchestrator(syncManager, dataProvider)

// Observe sync progress
orchestrator.syncProgress.collect { progress ->
    when (progress) {
        is SyncProgress.Initializing -> println("Initializing sync...")
        is SyncProgress.ExchangingDeleteLogs -> println("Exchanging delete logs...")
        is SyncProgress.SendingData -> println("Sending data...")
        is SyncProgress.ReceivingData -> println("Receiving data...")
        is SyncProgress.MergingData -> println("Merging data...")
        is SyncProgress.Completed -> println("Sync completed!")
        is SyncProgress.Error -> println("Error: ${progress.message}")
    }
}

// Observe merged data
orchestrator.mergedData.collect { data ->
    data?.let { syncData ->
        // Apply merged data to your database
        dataProvider.applyMergedData(syncData)
    }
}
```

### Server Mode (Host Device)

```kotlin
// Start server and get connection info
val serverInfo = syncManager.startServer(port = 7484)
println("Server started at: ${serverInfo.fullAddress}")

// Generate QR code data for client to scan
val qrData = QRUtils.createQRData(serverInfo)
// Use qrData string to generate QR code in your UI

// Start sync process
orchestrator.startSync()
```

### Client Mode (Connecting Device)

```kotlin
// Parse QR code data from scanned QR code
val serverInfo = QRUtils.parseQRData(scannedQRData)

// Connect to server
syncManager.connectToServer(serverInfo.fullAddress)

// Start sync process
orchestrator.startSync()
```

### Implementing SyncDataProvider

```kotlin
class YourSyncDataProvider : SyncDataProvider {
    
    override suspend fun getDeleteLogs(): List<DeleteLog> {
        // Return delete logs from your local database
        return database.getDeleteLogs().map { log ->
            DeleteLog(
                logId = log.logId,
                entityId = log.entityId,
                entityType = when (log.entityType) {
                    "bookmark" -> EntityType.BOOKMARK
                    "collection" -> EntityType.COLLECTION
                    "all" -> EntityType.ALL
                    else -> EntityType.BOOKMARK
                },
                actionType = when (log.actionType) {
                    "created" -> ActionType.CREATED
                    "updated" -> ActionType.UPDATED
                    "deleted" -> ActionType.DELETED
                    "deletedAll" -> ActionType.DELETED_ALL
                    else -> ActionType.DELETED
                },
                timestamp = log.timestamp.time,
                fieldChangesJSON = log.fieldChangesJSON
            )
        }
    }
    
    override suspend fun getFullSyncData(): SyncData {
        // Return complete sync data from your local database
        val collections = database.getCollections().map { collection ->
            SyncCollection(
                collectionId = collection.collectionId,
                label = collection.label,
                icon = collection.icon,
                createdAt = collection.createdAt.toISOString(),
                editedAt = collection.editedAt.toISOString(),
                color = collection.color.rawValue,
                type = collection.type,
                bookmarks = collection.bookmarks?.map { it.bookmarkId } ?: emptyList(),
                version = collection.version
            )
        }
        
        val bookmarks = database.getBookmarks().map { bookmark ->
            SyncBookmark(
                bookmarkId = bookmark.bookmarkId,
                label = bookmark.label,
                bookmarkDescription = bookmark.bookmarkDescription,
                link = bookmark.link,
                domain = bookmark.domain,
                createdAt = bookmark.createdAt.toISOString(),
                editedAt = bookmark.editedAt.toISOString(),
                imageUrl = bookmark.imageUrl,
                iconUrl = bookmark.iconUrl,
                videoUrl = bookmark.videoUrl,
                readableHTML = bookmark.readableHTML,
                type = bookmark.type,
                version = bookmark.version,
                imageData = ImageUtils.encodeImageData(bookmark.imageDataHolder),
                iconData = ImageUtils.encodeImageData(bookmark.iconDataHolder)
            )
        }
        
        return SyncData(
            id = System.currentTimeMillis().toString(),
            exportedFrom = "YourApp",
            collections = collections,
            bookmarks = bookmarks,
            deleteLogs = getDeleteLogs()
        )
    }
    
    override suspend fun applyMergedData(data: SyncData) {
        // Apply merged data to your local database
        // This is where you update your database with the synced data
        
        // Clear existing data
        database.clearAll()
        
        // Apply collections
        data.collections.forEach { collection ->
            database.insertCollection(collection)
        }
        
        // Apply bookmarks
        data.bookmarks.forEach { bookmark ->
            val bookmarkModel = BookmarkModel(
                bookmarkId = bookmark.bookmarkId ?: UUID.randomUUID().toString(),
                label = bookmark.label ?: bookmark.link,
                bookmarkDescription = bookmark.bookmarkDescription ?: "",
                link = bookmark.link,
                domain = bookmark.domain ?: "",
                createdAt = bookmark.createdAt?.toDate() ?: Date(),
                editedAt = bookmark.editedAt?.toDate() ?: Date(),
                imageDataHolder = ImageUtils.decodeImageData(bookmark.imageData),
                iconDataHolder = ImageUtils.decodeImageData(bookmark.iconData),
                imageUrl = bookmark.imageUrl,
                iconUrl = bookmark.iconUrl,
                videoUrl = bookmark.videoUrl,
                readableHTML = bookmark.readableHTML,
                type = bookmark.type ?: 1,
                version = bookmark.version ?: 0
            )
            database.insertBookmark(bookmarkModel)
        }
        
        // Apply delete logs
        data.deleteLogs.forEach { deleteLog ->
            database.insertDeleteLog(deleteLog)
        }
    }
}
```

### QR Code Integration

```kotlin
// Generate QR code data
val qrData = QRUtils.createQRData(serverInfo)

// Validate QR code data before processing
if (QRUtils.isValidQRData(scannedData)) {
    val serverInfo = QRUtils.parseQRData(scannedData)
    // Connect to server
} else {
    // Handle invalid QR code
}
```

## Data Structures

### SyncData
Complete sync data containing collections, bookmarks, and delete logs.

### SyncCollection
Collection data with metadata and bookmark references.

### SyncBookmark
Bookmark data including Base64-encoded image data.

### DeleteLog
Delete operation log for efficient sync optimization.

## Platform Support

| Platform | Server Support | Client Support | Notes |
|----------|----------------|----------------|-------|
| **Android** | ✅ Yes         | ✅ Yes         | Full support |
| **iOS**      | ❌ No          | ✅ Yes         | iOS restrictions prevent server mode |
| **macOS**    | ✅ Yes         | ✅ Yes         | Full support |
| **JVM**      | ✅ Yes         | ✅ Yes         | Full support |

## Dependencies

The library uses the following dependencies:

- **Ktor**: WebSocket communication
- **Kotlinx Serialization**: JSON serialization
- **Kotlinx Coroutines**: Asynchronous operations
- **Kotlinx DateTime**: Timestamp handling
- **Platform-specific Base64**: Image data encoding/decoding

## Network Requirements

- Both devices must be on the same local network
- Firewall must allow connections on the specified port (default: 7484)
- Network discovery must be enabled

## Security Considerations

- Communication is unencrypted by default (local network only)
- No authentication mechanism (trusts local network)
- Consider implementing encryption for sensitive data
- Validate all incoming data before processing
- Image data is Base64 encoded for transmission

## Error Handling

```kotlin
try {
    orchestrator.startSync()
} catch (e: Exception) {
    when (e) {
        is IllegalArgumentException -> println("Invalid sync data")
        is IllegalStateException -> println("Sync not ready")
        else -> println("Sync failed: ${e.message}")
    }
}
```

## License

MIT License - see LICENSE file for details.

### Publish to MavenLocal

1) Run `./gradlew :YABASyncCore:publishToMavenLocal`
2) Open `~/.m2/repository/dev/subfly/yabasync/`
