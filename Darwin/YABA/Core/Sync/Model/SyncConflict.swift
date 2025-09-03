//
//  SyncConflict.swift
//  YABA
//
//  Created by Ali Taha on 17.08.2025.
//

import Foundation

/// Represents a sync conflict that needs resolution
struct SyncConflict {
    let type: ConflictType
    let itemId: String
    let localTimestamp: Date
    let remoteTimestamp: Date
    let resolution: ConflictResolution
    
    enum ConflictType {
        case bookmark
        case collection
    }
    
    enum ConflictResolution {
        case keepLocal
        case keepRemote
        case needsManualResolution
    }
}
