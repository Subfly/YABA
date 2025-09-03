//
//  SyncModels.swift
//  YABA
//
//  Created by Ali Taha on 31.05.2025.
//

import Foundation

// MARK: - Sync Request Flow

/// Initial message asking another device if they want to sync
struct SyncRequestMessage: Codable, Equatable {
    let requestId: String
    let fromDeviceId: String
    let fromDeviceName: String
    let timestamp: String
    let message: String?
}

/// Response to a sync request (accept/reject)
struct SyncRequestResponse: Codable, Equatable {
    let requestId: String
    let accepted: Bool
    let fromDeviceId: String
    let fromDeviceName: String
    let timestamp: String
}

// MARK: - Sync Data Exchange

/// Actual sync data sent between devices
struct SyncDataMessage: Codable, Equatable {
    let deviceId: String
    let deviceName: String
    let timestamp: String
    let ipAddress: String
    let bookmarks: [YabaCodableBookmark]
    let collections: [YabaCodableCollection]
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
}

/// Sync response used by DataManager
struct SyncResponse: Codable, Equatable {
    let deviceId: String
    let deviceName: String
    let timestamp: String
    let bookmarks: [YabaCodableBookmark]
    let collections: [YabaCodableCollection]
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
