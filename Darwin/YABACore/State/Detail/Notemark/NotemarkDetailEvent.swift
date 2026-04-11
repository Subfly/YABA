//
//  NotemarkDetailEvent.swift
//  YABACore
//

import Foundation

public enum NotemarkDetailEvent: Sendable {
    case onInit(bookmarkId: String)
    case onSave(documentJson: String, usedInlineAssetSrcs: [String])
    case onDeleteBookmark(bookmarkId: String)
    case onRequestNotificationPermission
    case onScheduleReminder(titleKey: String, messageKey: String, fireAt: Date)
    case onCancelReminder
    case onPickImageFromGallery
    case onCaptureImageFromCamera
    case onConsumedInlineImageInsert
    case onWebInitialContentLoad(resultJson: String?)
    case onTocChanged(tocJson: String?)
    case onNavigateToTocItem(id: String, extrasJson: String?)
    case onClearTocNavigation
    case onExportMarkdownReady(String)
    case onExportPdfReady(base64: String)

    case saveDocument(bookmarkId: String, data: Data)
    case ensureReadableMirror(bookmarkId: String, versionId: String, json: String)
}
