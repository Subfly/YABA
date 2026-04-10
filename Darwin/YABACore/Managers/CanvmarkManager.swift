//
//  CanvmarkManager.swift
//  YABACore
//
//  Canvas bookmark subtype (`CanvasBookmarkModel` + payload). Compose `CanvmarkManager` parity.
//

import Foundation
import SwiftData

public enum CanvmarkManager {
    public static func queueSaveCanvasSceneData(
        bookmarkId: String,
        sceneData: Data,
        touchEditedAt: Bool = true
    ) {
        YabaCoreOperationQueue.shared.queue(name: "SaveCanvmarkScene:\(bookmarkId)") { context in
            try saveCanvasSceneInternal(
                bookmarkId: bookmarkId,
                sceneData: sceneData,
                touchEditedAt: touchEditedAt,
                context: context
            )
        }
    }

    public static func queueCreateOrUpdateCanvasDetails(bookmarkId: String) {
        YabaCoreOperationQueue.shared.queue(name: "CreateOrUpdateCanvasDetails:\(bookmarkId)") { context in
            try createOrUpdateCanvasDetailsInternal(bookmarkId: bookmarkId, context: context)
        }
    }

    private static func saveCanvasSceneInternal(
        bookmarkId: String,
        sceneData: Data,
        touchEditedAt: Bool,
        context: ModelContext
    ) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
            return
        }
        let canvas = try ensureCanvasDetail(bookmark: bookmark, context: context)
        let payload = try ensureCanvasPayload(canvas: canvas, context: context)
        payload.sceneData = sceneData
        if touchEditedAt {
            bookmark.editedAt = .now
        }
    }

    private static func createOrUpdateCanvasDetailsInternal(bookmarkId: String, context: ModelContext) throws {
        guard let bookmark = try YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context) else {
            return
        }
        let canvas = try ensureCanvasDetail(bookmark: bookmark, context: context)
        _ = try ensureCanvasPayload(canvas: canvas, context: context)
        bookmark.editedAt = .now
    }

    private static func ensureCanvasDetail(bookmark: BookmarkModel, context: ModelContext) throws -> CanvasBookmarkModel {
        if let canvas = bookmark.canvasDetail {
            return canvas
        }
        let canvas = CanvasBookmarkModel(bookmark: bookmark)
        context.insert(canvas)
        bookmark.canvasDetail = canvas
        return canvas
    }

    private static func ensureCanvasPayload(canvas: CanvasBookmarkModel, context: ModelContext) throws -> CanvasBookmarkPayloadModel {
        if let payload = canvas.payload {
            return payload
        }
        let payload = CanvasBookmarkPayloadModel(canvasBookmark: canvas)
        context.insert(payload)
        canvas.payload = payload
        return payload
    }
}
