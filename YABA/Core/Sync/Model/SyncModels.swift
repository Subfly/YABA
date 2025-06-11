//
//  SyncModels.swift
//  YABA
//
//  Created by Ali Taha on 31.05.2025.
//

struct SyncRequest: Codable {
    let deviceId: String
    let deviceName: String
    let timestamp: String
    let ipAddress: String
    let bookmarks: [YabaCodableBookmark]
    let collections: [YabaCodableCollection]
}

struct SyncResponse: Codable {
    let deviceId: String
    let deviceName: String
    let timestamp: String
    let bookmarks: [YabaCodableBookmark]
    let collections: [YabaCodableCollection]
}
