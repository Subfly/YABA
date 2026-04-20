//
//  ReadableContentManager.swift
//  YABACore
//
//  Readable pipeline helpers (Compose `ReadableContentManager`). SwiftData stores readable bodies
//  in `ReadableVersionPayloadModel.documentJson` (TipTap/ProseMirror JSON for links and notes).
//

import Foundation
import SwiftData

public enum ReadableContentManager {
    /// When a docmark has no readable rows yet, inserts a minimal JSON placeholder version so
    /// annotations can attach (Compose `ensureDocmarkAnnotationReadableVersionIfNeeded`).
    public static func queueEnsureDocmarkReadableVersionIfNeeded(bookmarkId: String) {
        CoreOperationQueue.shared.queue(name: "EnsureDocmarkReadable:\(bookmarkId)") { context in
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
        CoreOperationQueue.shared.queue(name: "SyncNotemarkReadable:\(bookmarkId)") { context in
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

    /// Inserts a **new** link readable version (e.g. first save or explicit “update readable” from detail).
    public static func queueSaveLinkReadableUnfurl(
        bookmarkId: String,
        readableVersionId: String,
        unfurl: ReadableUnfurl
    ) {
        CoreOperationQueue.shared.queue(name: "SaveLinkReadable:\(bookmarkId):\(readableVersionId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                return
            }
            let data = Data(unfurl.documentJson.utf8)
            let payload = ReadableVersionPayloadModel(documentJson: data, readableVersion: nil)
            context.insert(payload)
            let version = ReadableVersionModel(
                readableVersionId: readableVersionId,
                createdAt: .now,
                relativePathHint: nil,
                payload: payload,
                bookmark: bookmark
            )
            context.insert(version)
            payload.readableVersion = version
            bookmark.readableVersions.append(version)
            for a in unfurl.assets {
                let row = ReadableInlineAssetModel(
                    assetId: a.assetId,
                    pathExtension: a.pathExtension,
                    bytes: a.bytes,
                    readableVersion: version
                )
                context.insert(row)
                version.inlineAssets.append(row)
            }
            bookmark.editedAt = .now
            ReadableAssetResolver.shared.register(unfurl: unfurl)
        }
    }

    /// Saves link readable content from the bookmark editor without creating a new version row when one already exists.
    /// - If the bookmark has no readable yet, inserts the first version.
    /// - Otherwise updates the latest readable version’s JSON and inline assets in place.
    public static func queueUpsertLinkReadableUnfurlFromBookmarkEditor(
        bookmarkId: String,
        unfurl: ReadableUnfurl
    ) {
        CoreOperationQueue.shared.queue(name: "UpsertLinkReadableEditor:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                return
            }
            let data = Data(unfurl.documentJson.utf8)
            if bookmark.readableVersions.isEmpty {
                let rv = UUID().uuidString
                let payload = ReadableVersionPayloadModel(documentJson: data, readableVersion: nil)
                context.insert(payload)
                let version = ReadableVersionModel(
                    readableVersionId: rv,
                    createdAt: .now,
                    relativePathHint: nil,
                    payload: payload,
                    bookmark: bookmark
                )
                context.insert(version)
                payload.readableVersion = version
                bookmark.readableVersions.append(version)
                for a in unfurl.assets {
                    let row = ReadableInlineAssetModel(
                        assetId: a.assetId,
                        pathExtension: a.pathExtension,
                        bytes: a.bytes,
                        readableVersion: version
                    )
                    context.insert(row)
                    version.inlineAssets.append(row)
                }
            } else if let version = bookmark.readableVersions.max(by: { $0.createdAt < $1.createdAt }) {
                if let payload = version.payload {
                    payload.documentJson = data
                } else {
                    let payload = ReadableVersionPayloadModel(documentJson: data, readableVersion: nil)
                    context.insert(payload)
                    version.payload = payload
                    payload.readableVersion = version
                }
                version.createdAt = .now
                for row in version.inlineAssets {
                    context.delete(row)
                }
                version.inlineAssets.removeAll()
                for a in unfurl.assets {
                    let row = ReadableInlineAssetModel(
                        assetId: a.assetId,
                        pathExtension: a.pathExtension,
                        bytes: a.bytes,
                        readableVersion: version
                    )
                    context.insert(row)
                    version.inlineAssets.append(row)
                }
            }
            bookmark.editedAt = .now
            ReadableAssetResolver.shared.register(unfurl: unfurl)
        }
    }
}
