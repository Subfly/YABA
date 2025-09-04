//
//  SyncModels.swift
//  YABA
//
//  Created by Ali Taha on 31.05.2025.
//

import Foundation

// MARK: - Message Type Discriminator

enum SyncMessageType: String, Codable {
    case syncRequest = "sync_request"
    case syncResponse = "sync_response"
    case syncData = "sync_data"
}

// MARK: - Sync Request Flow

/// Initial message asking another device if they want to sync
struct SyncRequestMessage: Codable, Equatable {
    let messageType: SyncMessageType
    let requestId: String
    let fromDeviceId: String
    let fromDeviceName: String
    let timestamp: String
    let message: String?
    
    init(requestId: String, fromDeviceId: String, fromDeviceName: String, timestamp: String, message: String? = nil) {
        self.messageType = .syncRequest
        self.requestId = requestId
        self.fromDeviceId = fromDeviceId
        self.fromDeviceName = fromDeviceName
        self.timestamp = timestamp
        self.message = message
    }
}

/// Response to a sync request (accept/reject)
struct SyncRequestResponse: Codable, Equatable {
    let messageType: SyncMessageType
    let requestId: String
    let accepted: Bool
    let fromDeviceId: String
    let fromDeviceName: String
    let timestamp: String
    
    init(requestId: String, accepted: Bool, fromDeviceId: String, fromDeviceName: String, timestamp: String) {
        self.messageType = .syncResponse
        self.requestId = requestId
        self.accepted = accepted
        self.fromDeviceId = fromDeviceId
        self.fromDeviceName = fromDeviceName
        self.timestamp = timestamp
    }
}

// MARK: - Sync Data Exchange

/// Actual sync data sent between devices
struct SyncDataMessage: Codable, Equatable {
    let messageType: SyncMessageType
    let deviceId: String
    let deviceName: String
    let timestamp: String
    let ipAddress: String
    let bookmarks: [YabaCodableBookmark]
    let collections: [YabaCodableCollection]
    let deletionLogs: [YabaCodableDeletionLog]
    
    init(deviceId: String, deviceName: String, timestamp: String, ipAddress: String, bookmarks: [YabaCodableBookmark], collections: [YabaCodableCollection], deletionLogs: [YabaCodableDeletionLog]) {
        self.messageType = .syncData
        self.deviceId = deviceId
        self.deviceName = deviceName
        self.timestamp = timestamp
        self.ipAddress = ipAddress
        self.bookmarks = bookmarks
        self.collections = collections
        self.deletionLogs = deletionLogs
    }
}

// MARK: - DataManager Compatibility Types

/// Sync request used by DataManager
struct SyncRequest: Codable, Equatable {
    let deviceId: String
    let deviceName: String
    let timestamp: String
    let ipAddress: String
    let bookmarks: [YabaCodableBookmark]
    let collections: [YabaCodableCollection]
    let deletionLogs: [YabaCodableDeletionLog]
}

/// Sync response used by DataManager
struct SyncResponse: Codable, Equatable {
    let deviceId: String
    let deviceName: String
    let timestamp: String
    let bookmarks: [YabaCodableBookmark]
    let collections: [YabaCodableCollection]
    let deletionLogs: [YabaCodableDeletionLog]
}

// MARK: - Deletion Log Support

/// Codable version of deletion log for sync
struct YabaCodableDeletionLog: Codable, Equatable {
    let entityId: String
    let entityType: String // "bookmark", "collection", "deleteAll"
    let timestamp: String
}

// MARK: - Simple Result Types

enum SyncStatus: Equatable {
    case idle
    case discovering
    case requesting
    case syncing
    case completed
    case failed(String)
}

struct SimpleSyncResult: Equatable {
    let success: Bool
    let mergedBookmarks: Int
    let mergedCollections: Int
    let error: String?
}
