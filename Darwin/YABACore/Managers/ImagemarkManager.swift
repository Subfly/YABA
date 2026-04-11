//
//  ImagemarkManager.swift
//  YABACore
//
//  Image bookmark subtype (`ImageBookmarkModel`). Compose `ImagemarkManager` parity.
//

import Foundation
import SwiftData

public enum ImagemarkManager {
    public static func queueCreateOrUpdateImageDetails(bookmarkId: String, summary: String?) {
        CoreOperationQueue.shared.queue(name: "CreateOrUpdateImageDetails:\(bookmarkId)") { context in
            try upsertImageDetails(bookmarkId: bookmarkId, summary: summary, context: context)
        }
    }

    private static func upsertImageDetails(bookmarkId: String, summary: String?, context: ModelContext) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
            return
        }
        let row: ImageBookmarkModel
        if let existing = bookmark.imageDetail {
            row = existing
        } else {
            let inserted = ImageBookmarkModel(summary: summary, bookmark: bookmark)
            context.insert(inserted)
            bookmark.imageDetail = inserted
            row = inserted
        }
        row.summary = summary?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        bookmark.editedAt = .now
    }
}
