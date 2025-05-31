//
//  SyncModels.swift
//  YABA
//
//  Created by Ali Taha on 31.05.2025.
//


import Vapor

struct SyncRequest: Content {
    let deviceId: String
    let deviceName: String
    let timestamp: String
    let ipAddress: String
    let bookmarks: [YabaCodableBookmark]
    let collections: [YabaCodableCollection]
}

struct SyncResponse: Content {
    let deviceId: String
    let deviceName: String
    let timestamp: String
    let bookmarks: [YabaCodableBookmark]
    let collections: [YabaCodableCollection]
}
