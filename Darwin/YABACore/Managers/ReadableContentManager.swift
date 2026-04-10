//
//  ReadableContentManager.swift
//  YABACore
//
//  Readable pipeline helpers (Compose `ReadableContentManager`). SwiftData stores document JSON
//  primarily in `ReadableVersionPayloadModel`; full filesystem mirror parity is future work.
//

import Foundation
import SwiftData

public enum ReadableContentManager {
    /// When a docmark has no readable rows yet, inserts a minimal JSON placeholder version so
    /// annotations can attach (Compose `ensureDocmarkAnnotationReadableVersionIfNeeded`).
    public static func queueEnsureDocmarkReadableVersionIfNeeded(bookmarkId: String) {
        YabaCoreOperationQueue.shared.queue(name: "EnsureDocmarkReadable:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                return
            }
            if !bookmark.readableVersions.isEmpty { return }
            let json = #"{"type":"doc","content":[]}"#
            let data = Data(json.utf8)
            let payload = ReadableVersionPayloadModel(documentJson: data, readableVersion: nil)
            context.insert(payload)
            let version = ReadableVersionModel(
                readableVersionId: UUID().uuidString,
                createdAt: .now,
                relativePathHint: nil,
                payload: payload,
                bookmark: bookmark
            )
            context.insert(version)
            payload.readableVersion = version
            bookmark.readableVersions.append(version)
            bookmark.editedAt = .now
        }
    }

    /// Persists notemark editor JSON into payload + ensures a readable version row exists for anchoring.
    public static func queueSyncNotemarkReadableMirror(bookmarkId: String, versionId: String, documentJson: String) {
        YabaCoreOperationQueue.shared.queue(name: "SyncNotemarkReadable:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                return
            }
            let data = Data(documentJson.utf8)
            if let existing = bookmark.readableVersions.first(where: { $0.readableVersionId == versionId }) {
                existing.payload?.documentJson = data
                existing.createdAt = .now
            } else {
                let payload = ReadableVersionPayloadModel(documentJson: data, readableVersion: nil)
                context.insert(payload)
                let version = ReadableVersionModel(
                    readableVersionId: versionId,
                    createdAt: .now,
                    relativePathHint: nil,
                    payload: payload,
                    bookmark: bookmark
                )
                context.insert(version)
                payload.readableVersion = version
                bookmark.readableVersions.append(version)
            }
            if let note = bookmark.noteDetail {
                note.readableVersionId = versionId
            }
            bookmark.editedAt = .now
        }
    }
}
