//
//  ImagemarkManager.swift
//  YABACore
//
//  Image bookmark subtype (`ImageBookmarkModel`). Compose `ImagemarkManager` parity.
//

import Foundation
import SwiftData

public enum ImagemarkManager {
    public static func queueCreateOrUpdateImageDetails(
        bookmarkId: String,
        summary: String?,
        originalImageData: Data? = nil
    ) {
        CoreOperationQueue.shared.queue(name: "CreateOrUpdateImageDetails:\(bookmarkId)") { context in
            try upsertImageDetails(
                bookmarkId: bookmarkId,
                summary: summary,
                originalImageData: originalImageData,
                context: context
            )
        }
    }

    private static func upsertImageDetails(
        bookmarkId: String,
        summary: String?,
        originalImageData: Data?,
        context: ModelContext
    ) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
            return
        }
        let row: ImageBookmarkModel
        if let existing = bookmark.imageDetail {
            row = existing
        } else {
            let inserted = ImageBookmarkModel(summary: summary, originalImageData: originalImageData, bookmark: bookmark)
            context.insert(inserted)
            bookmark.imageDetail = inserted
            row = inserted
        }
        if let summary {
            row.summary = summary.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        }
        if let originalImageData {
            row.originalImageData = originalImageData
        }
        bookmark.editedAt = .now
    }
}
