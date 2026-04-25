//
//  ReadableVersionManager.swift
//  YABACore
//
//  Readable version rows (Compose `ReadableContentManager` / readable pipeline — extend as needed).
//

import Foundation
import SwiftData

public enum ReadableVersionManager {
    public static func queueInsertReadableVersion(
        bookmarkId: String,
        readableVersionId: String = UUID().uuidString,
        relativePathHint: String? = nil,
        html: Data? = nil
    ) {
        CoreOperationQueue.shared.queue(name: "InsertReadableVersion:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                return
            }
            let payload = ReadableVersionPayloadModel(html: html, readableVersion: nil)
            context.insert(payload)
            let version = ReadableVersionModel(
                readableVersionId: readableVersionId,
                createdAt: .now,
                relativePathHint: relativePathHint,
                payload: payload,
                bookmark: bookmark
            )
            context.insert(version)
            payload.readableVersion = version
            bookmark.readableVersions.append(version)
            bookmark.editedAt = .now
        }
    }

    public static func queueDeleteReadableVersion(readableVersionId: String) {
        CoreOperationQueue.shared.queue(name: "DeleteReadableVersion:\(readableVersionId)") { context in
            let p = #Predicate<ReadableVersionModel> { $0.readableVersionId == readableVersionId }
            var d = FetchDescriptor(predicate: p)
            d.fetchLimit = 1
            guard let version = try context.fetch(d).first else { return }
            if let payload = version.payload {
                context.delete(payload)
            }
            context.delete(version)
            version.bookmark?.editedAt = .now
        }
    }
}
