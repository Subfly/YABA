//
//  ReadableContentManager.swift
//  YABACore
//
//  Readable pipeline helpers (Compose `ReadableContentManager` parity). One readable payload
//  per link on `LinkBookmarkModel`; notemark body lives in `NoteBookmarkPayloadModel`.
//

import Foundation
import SwiftData

public enum ReadableContentManager {
    /// Persists the notemark editor JSON; body is stored only on the note payload (no separate readable version).
    public static func queueSyncNotemarkReadableMirror(bookmarkId: String, html: String) {
        CoreOperationQueue.shared.queue(name: "SyncNotemarkReadable:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                return
            }
            let data = Data(html.utf8)
            guard let note = bookmark.noteDetail else { return }
            if let payload = note.payload {
                payload.documentBody = data
            } else {
                let payload = NoteBookmarkPayloadModel(documentBody: data, noteBookmark: note)
                context.insert(payload)
                note.payload = payload
            }
            bookmark.editedAt = .now
        }
    }

    /// Replaces the link bookmark readable with an unfurl (body + inline assets).
    public static func queueSaveLinkReadableUnfurl(
        bookmarkId: String,
        unfurl: ReadableUnfurl
    ) {
        CoreOperationQueue.shared.queue(name: "SaveLinkReadableUnfurl:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context),
                  let link = bookmark.linkDetail
            else {
                return
            }
            applyUnfurl(unfurl, to: link, context: context)
            bookmark.editedAt = .now
            ReadableAssetResolver.shared.register(unfurl: unfurl)
        }
    }

    /// Inserts or updates the single link readable (bookmark editor) in place.
    public static func queueUpsertLinkReadableUnfurlFromBookmarkEditor(
        bookmarkId: String,
        unfurl: ReadableUnfurl
    ) {
        CoreOperationQueue.shared.queue(name: "UpsertLinkReadableEditor:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context),
                  let link = bookmark.linkDetail
            else {
                return
            }
            applyUnfurl(unfurl, to: link, context: context)
            bookmark.editedAt = .now
            ReadableAssetResolver.shared.register(unfurl: unfurl)
        }
    }

    /// Persists `Data` as the link readable body (e.g. ad-hoc saves) without changing inline assets.
    public static func queueSetLinkReadableDocumentData(bookmarkId: String, data: Data?) {
        CoreOperationQueue.shared.queue(name: "SetLinkReadableData:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context),
                  let link = bookmark.linkDetail
            else {
                return
            }
            if let data {
                link.markdown = String(data: data, encoding: .utf8) ?? ""
            } else {
                link.markdown = ""
            }
            bookmark.editedAt = .now
        }
    }

    /// After a WebView edit (annotations), writes the current JSON back to SwiftData.
    public static func queueUpdateReadableBodyFromWebEditor(bookmarkId: String, html: String) {
        CoreOperationQueue.shared.queue(name: "ReadableFromWeb:\(bookmarkId)") { context in
            guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
                return
            }
            if let link = bookmark.linkDetail {
                link.markdown = html
            } else if let note = bookmark.noteDetail {
                let data = Data(html.utf8)
                if let payload = note.payload {
                    payload.documentBody = data
                } else {
                    let payload = NoteBookmarkPayloadModel(documentBody: data, noteBookmark: note)
                    context.insert(payload)
                    note.payload = payload
                }
            }
            bookmark.editedAt = .now
        }
    }

    // MARK: - Private

    private static func applyUnfurl(_ unfurl: ReadableUnfurl, to link: LinkBookmarkModel, context: ModelContext) {
        link.markdown = unfurl.markdown
        for row in link.inlineAssets {
            context.delete(row)
        }
        link.inlineAssets.removeAll()
        for a in unfurl.assets {
            let row = InlineAssetModel(
                assetId: a.assetId,
                pathExtension: a.pathExtension,
                bytes: a.bytes,
                linkBookmark: link
            )
            context.insert(row)
            link.inlineAssets.append(row)
        }
    }
}
