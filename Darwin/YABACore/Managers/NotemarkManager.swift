//
//  NotemarkManager.swift
//  YABACore
//
//  Note bookmark subtype (`NoteBookmarkModel` + payload). Compose `NotemarkManager` parity
//  (SwiftData stores document body in `NoteBookmarkPayloadModel`; filesystem mirrors deferred).
//

import Foundation
import SwiftData

public enum NotemarkManager {
    public static func queueSaveNoteDocumentData(
        bookmarkId: String,
        documentBody: Data?,
        touchEditedAt: Bool = true
    ) {
        CoreOperationQueue.shared.queue(name: "SaveNotemarkDocument:\(bookmarkId)") { context in
            try saveNoteDocumentInternal(
                bookmarkId: bookmarkId,
                documentBody: documentBody,
                touchEditedAt: touchEditedAt,
                context: context
            )
        }
    }

    public static func queueCreateOrUpdateNoteDetails(bookmarkId: String) {
        CoreOperationQueue.shared.queue(name: "CreateOrUpdateNoteDetails:\(bookmarkId)") { context in
            try createOrUpdateNoteDetailsInternal(bookmarkId: bookmarkId, context: context)
        }
    }

    private static func saveNoteDocumentInternal(
        bookmarkId: String,
        documentBody: Data?,
        touchEditedAt: Bool,
        context: ModelContext
    ) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
            return
        }
        let note = try ensureNoteDetail(bookmark: bookmark, context: context)
        let payload = try ensureNotePayload(noteDetail: note, context: context)
        payload.documentBody = documentBody
        if touchEditedAt {
            bookmark.editedAt = .now
        }
    }

    private static func createOrUpdateNoteDetailsInternal(
        bookmarkId: String,
        context: ModelContext
    ) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
            return
        }
        let note = try ensureNoteDetail(bookmark: bookmark, context: context)
        _ = try ensureNotePayload(noteDetail: note, context: context)
        bookmark.editedAt = .now
    }

    private static func ensureNoteDetail(bookmark: BookmarkModel, context: ModelContext) throws -> NoteBookmarkModel {
        if let note = bookmark.noteDetail {
            return note
        }
        let note = NoteBookmarkModel(bookmark: bookmark)
        context.insert(note)
        bookmark.noteDetail = note
        return note
    }

    private static func ensureNotePayload(noteDetail: NoteBookmarkModel, context: ModelContext) throws -> NoteBookmarkPayloadModel {
        if let payload = noteDetail.payload {
            return payload
        }
        let payload = NoteBookmarkPayloadModel(noteBookmark: noteDetail)
        context.insert(payload)
        noteDetail.payload = payload
        return payload
    }
}
