//
//  ImagemarkManager.swift
//  YABACore
//
//  Image bookmark subtype (`ImageBookmarkModel`). Compose `ImagemarkManager` parity.
//

import Foundation
import SwiftData

/// Image bytes + label for sharing or exporting an imagemark bookmark.
public struct ImagemarkExportPayload: Sendable {
    public let imageData: Data
    public let label: String

    public init(imageData: Data, label: String) {
        self.imageData = imageData
        self.label = label
    }
}

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

    /// Loads image bytes and label for share / save-to-photos flows.
    public static func fetchExportPayload(bookmarkId: String) async throws -> ImagemarkExportPayload? {
        try await withCheckedThrowingContinuation { cont in
            var result: ImagemarkExportPayload?
            CoreOperationQueue.shared.queue(name: "ImagemarkExportPayload:\(bookmarkId)") { context in
                guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                    result = nil
                    return
                }
                let data = bookmark.imageDetail?.originalImageData ?? bookmark.imagePayload?.bytes
                guard let data, !data.isEmpty else {
                    result = nil
                    return
                }
                result = ImagemarkExportPayload(imageData: data, label: bookmark.label)
            } completion: { error in
                if let error {
                    cont.resume(throwing: error)
                } else {
                    cont.resume(returning: result)
                }
            }
        }
    }
}
