//
//  FolderManager.swift
//  YABACore
//
//  Folder tree + system folder semantics aligned with Compose `FolderManager` (not legacy Darwin DnD).
//

import Foundation
import SwiftData

public enum FolderManager {
    // MARK: - System folder

    public static func queueEnsureUncategorizedFolderExists() {
        CoreOperationQueue.shared.queue(name: "EnsureUncategorizedFolder") { context in
            try ensureUncategorizedFolderInternal(context: context)
        }
    }

    /// Creates a user folder (system folders cannot be created here).
    public static func queueCreateFolder(
        folderId: String = UUID().uuidString,
        label: String,
        folderDescription: String? = nil,
        icon: String = "folder-01",
        colorRaw: Int = 0,
        parentFolderId: String?
    ) {
        CoreOperationQueue.shared.queue(name: "CreateFolder:\(folderId)") { context in
            try createFolderInternal(
                folderId: folderId,
                label: label,
                folderDescription: folderDescription,
                icon: icon,
                colorRaw: colorRaw,
                parentFolderId: parentFolderId,
                context: context
            )
        }
    }

    public static func queueUpdateFolderMetadata(
        folderId: String,
        label: String,
        folderDescription: String?,
        icon: String,
        colorRaw: Int
    ) {
        CoreOperationQueue.shared.queue(name: "UpdateFolder:\(folderId)") { context in
            guard let folder = try YabaCorePersistenceHelpers.folder(folderId: folderId, context: context) else { return }
            folder.label = label
            folder.folderDescription = folderDescription
            folder.icon = icon
            folder.colorRaw = colorRaw
            folder.editedAt = .now
        }
    }

    /// Moves a folder under a new parent. System folders cannot be moved.
    public static func queueMoveFolder(folderId: String, newParentFolderId: String?) {
        CoreOperationQueue.shared.queue(name: "MoveFolder:\(folderId)") { context in
            try moveFolderInternal(folderId: folderId, newParentFolderId: newParentFolderId, context: context)
        }
    }

    public static func queueDeleteFolder(folderId: String) {
        CoreOperationQueue.shared.queue(name: "DeleteFolder:\(folderId)") { context in
            try deleteFolderCascadeInternal(rootFolderId: folderId, context: context)
        }
    }

    // MARK: - Internal

    private static func ensureUncategorizedFolderInternal(context: ModelContext) throws {
        let id = Constants.Folder.Uncategorized.id
        if try YabaCorePersistenceHelpers.folder(folderId: id, context: context) != nil {
            return
        }
        let now = Date.now
        let folder = FolderModel(
            folderId: id,
            label: Constants.Folder.Uncategorized.name,
            folderDescription: Constants.Folder.Uncategorized.descriptionText,
            icon: Constants.Folder.Uncategorized.icon,
            colorRaw: 1,
            createdAt: now,
            editedAt: now,
            isHidden: false,
            parent: nil,
            children: [],
            bookmarks: []
        )
        context.insert(folder)
    }

    private static func createFolderInternal(
        folderId: String,
        label: String,
        folderDescription: String?,
        icon: String,
        colorRaw: Int,
        parentFolderId: String?,
        context: ModelContext
    ) throws {
        precondition(!label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        let parent: FolderModel?
        if let parentFolderId {
            parent = try YabaCorePersistenceHelpers.folder(folderId: parentFolderId, context: context)
        } else {
            parent = nil
        }
        let now = Date.now
        let folder = FolderModel(
            folderId: folderId,
            label: label,
            folderDescription: folderDescription,
            icon: icon,
            colorRaw: colorRaw,
            createdAt: now,
            editedAt: now,
            isHidden: false,
            parent: parent,
            children: [],
            bookmarks: []
        )
        context.insert(folder)
    }

    private static func moveFolderInternal(
        folderId: String,
        newParentFolderId: String?,
        context: ModelContext
    ) throws {
        if Constants.Folder.isSystemFolder(folderId) { return }
        guard let folder = try YabaCorePersistenceHelpers.folder(folderId: folderId, context: context) else { return }
        let newParent: FolderModel?
        if let newParentFolderId {
            newParent = try YabaCorePersistenceHelpers.folder(folderId: newParentFolderId, context: context)
        } else {
            newParent = nil
        }
        folder.parent = newParent
        folder.editedAt = .now
    }

    private static func deleteFolderCascadeInternal(rootFolderId: String, context: ModelContext) throws {
        if Constants.Folder.isSystemFolder(rootFolderId) { return }
        guard let folder = try YabaCorePersistenceHelpers.folder(folderId: rootFolderId, context: context) else { return }
        let bookmarkIds = collectBookmarkIdsInSubtree(folder: folder)
        ReminderManager.cancelReminders(bookmarkIds: bookmarkIds)
        context.delete(folder)
    }

    private static func collectBookmarkIdsInSubtree(folder: FolderModel) -> [String] {
        var ids = folder.bookmarks.map(\.bookmarkId)
        for child in folder.children {
            ids += collectBookmarkIdsInSubtree(folder: child)
        }
        return ids
    }
}
