//
//  BookmarkCreationFolderResolution.swift
//  YABACore
//
//  Parity with Compose bookmark creation: default folder selection + virtual Uncategorized
//  before the system folder row exists in SwiftData.
//

import Foundation
import SwiftData

public enum BookmarkCreationFolderResolution {
    /// Resolves folder id for a **new** bookmark (not editing). If `preselectedFolderId` is missing
    /// or does not exist in the store, returns the stable Uncategorized id and marks creation required
    /// on save (Compose `uncategorizedFolderCreationRequired`).
    @MainActor
    public static func resolveForNewBookmark(
        modelContext: ModelContext,
        preselectedFolderId: String?
    ) -> (selectedFolderId: String, uncategorizedFolderCreationRequired: Bool) {
        if let raw = preselectedFolderId?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty {
            let folderId = raw
            var descriptor = FetchDescriptor<FolderModel>(
                predicate: #Predicate<FolderModel> { $0.folderId == folderId }
            )
            descriptor.fetchLimit = 1
            if (try? modelContext.fetch(descriptor).first) != nil {
                return (folderId, false)
            }
        }
        return (Constants.Folder.Uncategorized.id, true)
    }
}
