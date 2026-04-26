//
//  SyncMergeResult.swift
//  YABA
//
//  Created by Ali Taha on 17.08.2025.
//

import Foundation

/// Result of merging sync data
struct SyncMergeResult {
    let mergedBookmarks: Int
    let mergedCollections: Int
    let conflicts: [SyncConflict]
}
