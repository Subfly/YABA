//
//  AllBookmarksManager.swift
//  YABACore
//
//  Compose `AllBookmarksManager` parity: bookmark CRUD, tags, pin system tag.
//  Reads stay in SwiftUI (`@Query`); this type is for mutations only.
//

import Foundation
import SwiftData

public enum AllBookmarksManager {
    // MARK: - Create / update

    /// Creates only the generic `BookmarkModel` metadata. Kind-specific subtype rows are added by
    /// the corresponding managers from state-machine flows.
    public static func queueCreateBookmark(
        bookmarkId: String = UUID().uuidString,
        folderId: String,
        kind: BookmarkKind,
        label: String,
        bookmarkDescription: String? = nil,
        isPinned: Bool = false,
        tagIds: [String] = []
    ) {
        precondition(!label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, "Bookmark label required")
        CoreOperationQueue.shared.queue(name: "CreateBookmarkBase:\(bookmarkId)") { context in
            try createBaseBookmarkInternal(
                bookmarkId: bookmarkId,
                folderId: folderId,
                kind: kind,
                label: label,
                bookmarkDescription: bookmarkDescription,
                isPinned: isPinned,
                tagIds: tagIds,
                context: context
            )
        }
    }

    public static func queueUpdateBookmarkMetadata(
        bookmarkId: String,
        folderId: String,
        kind: BookmarkKind,
        label: String,
        bookmarkDescription: String?,
        isPinned: Bool,
        tagIds: [String]?
    ) {
        precondition(!label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, "Bookmark label required")
        CoreOperationQueue.shared.queue(name: "UpdateBookmark:\(bookmarkId)") { context in
            try updateBookmarkMetadataInternal(
                bookmarkId: bookmarkId,
                folderId: folderId,
                kind: kind,
                label: label,
                bookmarkDescription: bookmarkDescription,
                isPinned: isPinned,
                tagIds: tagIds,
                context: context
            )
        }
    }

    public static func queueTouchEditedAt(bookmarkId: String) {
        CoreOperationQueue.shared.queue(name: "TouchBookmarkEditedAt:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else { return }
            bookmark.editedAt = .now
        }
    }

    /// Card preview image / favicon bytes on the base bookmark.
    public static func queueSetBookmarkPreviewAssets(bookmarkId: String, imageBytes: Data?, iconBytes: Data?) {
        CoreOperationQueue.shared.queue(name: "PreviewAssets:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                return
            }
            if let imageBytes {
                let toStore = YabaImageCompression.compressDataPreservingFormat(imageBytes)
                if let existing = bookmark.imagePayload {
                    existing.bytes = toStore
                } else {
                    let p = BookmarkImagePayloadModel(bytes: toStore, bookmark: bookmark)
                    context.insert(p)
                    bookmark.imagePayload = p
                }
            }
            if let iconBytes {
                let toStore = YabaImageCompression.compressDataPreservingFormat(iconBytes)
                if let existing = bookmark.iconPayload {
                    existing.bytes = toStore
                } else {
                    let p = BookmarkIconPayloadModel(bytes: toStore, bookmark: bookmark)
                    context.insert(p)
                    bookmark.iconPayload = p
                }
            }
            bookmark.editedAt = .now
        }
    }

    public static func queueRecordBookmarkView(bookmarkId: String) {
        CoreOperationQueue.shared.queue(name: "RecordBookmarkView:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else { return }
            bookmark.viewCount += 1
            bookmark.editedAt = .now
        }
    }

    // MARK: - Pin

    public static func queueToggleBookmarkPinned(bookmarkId: String) {
        CoreOperationQueue.shared.queue(name: "TogglePin:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else { return }
            let newPinned = !bookmark.isPinned
            bookmark.isPinned = newPinned
            bookmark.editedAt = .now
            try syncPinnedSystemTag(for: bookmark, isPinned: newPinned, context: context)
        }
    }

    // MARK: - Move / delete

    public static func queueMoveBookmarksToFolder(bookmarkIds: [String], targetFolderId: String) {
        guard !bookmarkIds.isEmpty else { return }
        CoreOperationQueue.shared.queue(name: "MoveBookmarks:\(bookmarkIds.count)") { context in
            try moveBookmarksToFolderInternal(bookmarkIds: bookmarkIds, targetFolderId: targetFolderId, context: context)
        }
    }

    public static func queueDeleteBookmarks(bookmarkIds: [String]) {
        guard !bookmarkIds.isEmpty else { return }
        CoreOperationQueue.shared.queue(name: "DeleteBookmarks:\(bookmarkIds.count)") { context in
            ReminderManager.cancelReminders(bookmarkIds: bookmarkIds)
            for id in bookmarkIds {
                if let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: id, context: context) {
                    context.delete(bookmark)
                }
            }
        }
    }

    /// Called from `FolderManager` cascade when a bookmark must be removed without extra queue nesting.
    public static func deleteBookmarkInContext(bookmarkId: String, context: ModelContext) throws {
        ReminderManager.cancelReminder(bookmarkId: bookmarkId)
        if let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) {
            context.delete(bookmark)
        }
    }

    // MARK: - Tags

    public static func queueAddTagToBookmark(tagId: String, bookmarkId: String) {
        CoreOperationQueue.shared.queue(name: "AddTag:\(tagId):\(bookmarkId)") { context in
            try addTagToBookmarkInternal(tagId: tagId, bookmarkId: bookmarkId, context: context)
        }
    }

    public static func queueRemoveTagFromBookmark(tagId: String, bookmarkId: String) {
        CoreOperationQueue.shared.queue(name: "RemoveTag:\(tagId):\(bookmarkId)") { context in
            try removeTagFromBookmarkInternal(tagId: tagId, bookmarkId: bookmarkId, context: context)
        }
    }

    // MARK: - Internal

    /// Inserts only `BookmarkModel` + tags + system tags.
    private static func createBaseBookmarkInternal(
        bookmarkId: String,
        folderId: String,
        kind: BookmarkKind,
        label: String,
        bookmarkDescription: String?,
        isPinned: Bool,
        tagIds: [String],
        context: ModelContext
    ) throws {
        guard let folder = try YabaCorePersistenceHelpers.folder(folderId: folderId, context: context) else {
            return
        }
        let now = Date.now
        let bookmark = BookmarkModel(
            bookmarkId: bookmarkId,
            kindRaw: kind.rawValue,
            label: label,
            bookmarkDescription: bookmarkDescription,
            createdAt: now,
            editedAt: now,
            viewCount: 0,
            isPinned: isPinned,
            folder: folder,
            tags: []
        )
        context.insert(bookmark)

        try applyTags(tagIds: tagIds, to: bookmark, context: context)
        try syncPinnedSystemTag(for: bookmark, isPinned: isPinned, context: context)
    }

    private static func updateBookmarkMetadataInternal(
        bookmarkId: String,
        folderId: String,
        kind: BookmarkKind,
        label: String,
        bookmarkDescription: String?,
        isPinned: Bool,
        tagIds: [String]?,
        context: ModelContext
    ) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else { return }
        guard let folder = try YabaCorePersistenceHelpers.folder(folderId: folderId, context: context) else { return }
        bookmark.folder = folder
        bookmark.kindRaw = kind.rawValue
        bookmark.label = label
        bookmark.bookmarkDescription = bookmarkDescription
        bookmark.isPinned = isPinned
        bookmark.editedAt = .now

        if let tagIds {
            let idSet = Set(tagIds)
            let current = Set(bookmark.tags.map(\.tagId))
            for remove in current where !idSet.contains(remove) {
                if let tag = try YabaCorePersistenceHelpers.tag(tagId: remove, context: context) {
                    bookmark.tags.removeAll { $0.tagId == tag.tagId }
                }
            }
            for add in idSet where !current.contains(add) {
                if let tag = try YabaCorePersistenceHelpers.tag(tagId: add, context: context) {
                    if !bookmark.tags.contains(where: { $0.tagId == tag.tagId }) {
                        bookmark.tags.append(tag)
                    }
                }
            }
        }
        try syncPinnedSystemTag(for: bookmark, isPinned: isPinned, context: context)
    }

    private static func moveBookmarksToFolderInternal(
        bookmarkIds: [String],
        targetFolderId: String,
        context: ModelContext
    ) throws {
        guard let folder = try YabaCorePersistenceHelpers.folder(folderId: targetFolderId, context: context) else { return }
        let now = Date.now
        for id in bookmarkIds {
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: id, context: context) else { continue }
            bookmark.folder = folder
            bookmark.editedAt = now
        }
    }

    private static func addTagToBookmarkInternal(tagId: String, bookmarkId: String, context: ModelContext) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context),
              let tag = try YabaCorePersistenceHelpers.tag(tagId: tagId, context: context)
        else { return }
        if !bookmark.tags.contains(where: { $0.tagId == tag.tagId }) {
            bookmark.tags.append(tag)
        }
        bookmark.editedAt = .now
    }

    private static func removeTagFromBookmarkInternal(tagId: String, bookmarkId: String, context: ModelContext) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else { return }
        bookmark.tags.removeAll { $0.tagId == tagId }
        bookmark.editedAt = .now
    }

    private static func applyTags(tagIds: [String], to bookmark: BookmarkModel, context: ModelContext) throws {
        for tagId in tagIds {
            if let tag = try YabaCorePersistenceHelpers.tag(tagId: tagId, context: context) {
                if !bookmark.tags.contains(where: { $0.tagId == tag.tagId }) {
                    bookmark.tags.append(tag)
                }
            }
        }
    }

    private static func syncPinnedSystemTag(for bookmark: BookmarkModel, isPinned: Bool, context: ModelContext) throws {
        let pinnedId = Constants.Tag.Pinned.id
        let pinnedTag = try TagManager.ensurePinnedTag(using: context)
        if isPinned {
            if !bookmark.tags.contains(where: { $0.tagId == pinnedId }) {
                bookmark.tags.append(pinnedTag)
            }
        } else {
            bookmark.tags.removeAll { $0.tagId == pinnedId }
        }
    }
}
