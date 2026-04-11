//
//  DocmarkManager.swift
//  YABACore
//
//  Document bookmark subtype (`DocBookmarkModel`). Compose `DocmarkManager` parity.
//

import Foundation
import SwiftData

public enum DocmarkManager {
    /// Ensures a `DocBookmarkModel` row exists after base bookmark creation (empty metadata).
    public static func queueEnsureInitialDocDetail(bookmarkId: String) {
        CoreOperationQueue.shared.queue(name: "EnsureInitialDocDetail:\(bookmarkId)") { context in
            try upsertDocDetails(
                bookmarkId: bookmarkId,
                summary: nil,
                docmarkType: nil,
                metadataTitle: nil,
                metadataDescription: nil,
                metadataAuthor: nil,
                metadataDate: nil,
                context: context
            )
        }
    }

    public static func queueCreateOrUpdateDocDetails(
        bookmarkId: String,
        summary: String? = nil,
        docmarkType: DocmarkType? = nil,
        metadataTitle: String? = nil,
        metadataDescription: String? = nil,
        metadataAuthor: String? = nil,
        metadataDate: String? = nil
    ) {
        CoreOperationQueue.shared.queue(name: "CreateOrUpdateDocDetails:\(bookmarkId)") { context in
            try upsertDocDetails(
                bookmarkId: bookmarkId,
                summary: summary,
                docmarkType: docmarkType,
                metadataTitle: metadataTitle,
                metadataDescription: metadataDescription,
                metadataAuthor: metadataAuthor,
                metadataDate: metadataDate,
                context: context
            )
        }
    }

    private static func upsertDocDetails(
        bookmarkId: String,
        summary: String?,
        docmarkType: DocmarkType?,
        metadataTitle: String?,
        metadataDescription: String?,
        metadataAuthor: String?,
        metadataDate: String?,
        context: ModelContext
    ) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
            return
        }
        let row: DocBookmarkModel
        if let existing = bookmark.docDetail {
            row = existing
        } else {
            let inserted = DocBookmarkModel(bookmark: bookmark)
            context.insert(inserted)
            bookmark.docDetail = inserted
            row = inserted
        }
        if let summary { row.summary = summary.nilIfEmpty }
        if let docmarkType { row.docmarkTypeRaw = docmarkType.rawValue }
        if let metadataTitle { row.metadataTitle = metadataTitle.nilIfEmpty }
        if let metadataDescription { row.metadataDescription = metadataDescription.nilIfEmpty }
        if let metadataAuthor { row.metadataAuthor = metadataAuthor.nilIfEmpty }
        if let metadataDate { row.metadataDate = metadataDate.nilIfEmpty }
        bookmark.editedAt = .now
    }
}
