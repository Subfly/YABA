//
//  GlobalDataManager.swift
//  YABACore
//
//  Process-wide data wipe (Compose `GlobalDataManager`). Deletes SwiftData rows; bookmark asset
//  directories on disk should be cleared in a future Darwin filesystem layer.
//

import Foundation
import SwiftData

public enum GlobalDataManager {
    /// Deletes all persisted v2 entities in the store accessed via `YabaCoreStore`.
    public static func queueWipeAllLocalData() {
        YabaCoreOperationQueue.shared.queue(name: "WipeAllLocalData") { context in
            try context.delete(TagModel.self)
            try context.delete(FolderModel.self)
        }
    }
}
