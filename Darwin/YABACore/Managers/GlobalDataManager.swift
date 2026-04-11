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
    /// Deletes all persisted v2 entities in the store accessed via `CoreStore`.
    public static func queueWipeAllLocalData() {
        CoreOperationQueue.shared.queue(name: "WipeAllLocalData") { context in
            ReminderManager.cancelAllReminders()
            try context.delete(model: TagModel.self)
            try context.delete(model: FolderModel.self)
        }
    }
}
