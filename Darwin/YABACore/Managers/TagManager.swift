//
//  TagManager.swift
//  YABACore
//
//  Compose-first tag rules. Methods taking `ModelContext` are synchronous and are intended to be
//  called from `CoreOperationQueue` work items (never nest another queue operation).
//

import Foundation
import SwiftData

public enum TagManager {
    // MARK: - System tags (Compose `CoreConstants.Tag`)

    /// Ensures the Pinned system tag row exists. Call only from the serial operation queue or a
    /// single-threaded migration context — **not** re-entrant through `CoreOperationQueue`.
    static func ensurePinnedTag(using context: ModelContext) throws -> TagModel {
        let id = Constants.Tag.Pinned.id
        if let existing = try YabaCorePersistenceHelpers.tag(tagId: id, context: context) {
            return existing
        }
        let now = Date.now
        let tag = TagModel(
            tagId: id,
            label: Constants.Tag.Pinned.name,
            icon: Constants.Tag.Pinned.icon,
            colorRaw: 2,
            createdAt: now,
            editedAt: now,
            isHidden: false,
            bookmarks: []
        )
        context.insert(tag)
        return tag
    }

    /// Ensures the Private system tag row exists (same threading rules as `ensurePinnedTag`).
    static func ensurePrivateTag(using context: ModelContext) throws -> TagModel {
        let id = Constants.Tag.Private.id
        if let existing = try YabaCorePersistenceHelpers.tag(tagId: id, context: context) {
            return existing
        }
        let now = Date.now
        let tag = TagModel(
            tagId: id,
            label: Constants.Tag.Private.name,
            icon: Constants.Tag.Private.icon,
            colorRaw: 3,
            createdAt: now,
            editedAt: now,
            isHidden: false,
            bookmarks: []
        )
        context.insert(tag)
        return tag
    }

    /// Queued: create a user tag row.
    public static func queueCreateTag(tagId: String = UUID().uuidString, label: String, icon: String, colorRaw: Int) {
        CoreOperationQueue.shared.queue(name: "CreateTag:\(tagId)") { context in
            if try YabaCorePersistenceHelpers.tag(tagId: tagId, context: context) != nil { return }
            let now = Date.now
            let tag = TagModel(
                tagId: tagId,
                label: label,
                icon: icon,
                colorRaw: colorRaw,
                createdAt: now,
                editedAt: now,
                isHidden: false,
                bookmarks: []
            )
            context.insert(tag)
        }
    }

    /// Queued: update label, icon, and color for an existing tag.
    public static func queueUpdateTagMetadata(tagId: String, label: String, icon: String, colorRaw: Int) {
        CoreOperationQueue.shared.queue(name: "UpdateTag:\(tagId)") { context in
            guard let tag = try YabaCorePersistenceHelpers.tag(tagId: tagId, context: context) else { return }
            tag.label = label
            tag.icon = icon
            tag.colorRaw = colorRaw
            tag.editedAt = .now
        }
    }

    /// Queued: hide non-user system tag or delete a normal tag (Compose parity).
    public static func queueDeleteOrHideTag(tagId: String) {
        CoreOperationQueue.shared.queue(name: "DeleteOrHideTag:\(tagId)") { context in
            try deleteOrHideTagInternal(tagId: tagId, context: context)
        }
    }

    private static func deleteOrHideTagInternal(tagId: String, context: ModelContext) throws {
        guard let tag = try YabaCorePersistenceHelpers.tag(tagId: tagId, context: context) else { return }
        if Constants.Tag.isSystemTag(tagId) {
            tag.isHidden = true
            tag.editedAt = .now
            return
        }
        for bookmark in tag.bookmarks {
            bookmark.tags.removeAll { $0.tagId == tagId }
            bookmark.editedAt = .now
        }
        context.delete(tag)
    }
}
