//
//  NotemarkDetailUIState.swift
//  YABACore
//

import Foundation

public struct NotemarkDetailUIState: Sendable {
    public var bookmarkId: String?
    public var inlineImageDocumentSrc: String?
    public var tocJson: String?
    public var pendingTocNavigationId: String?
    public var pendingTocNavigationExtrasJson: String?
    public var lastExportMarkdown: String?
    public var lastExportPdfBase64: String?
    public var reminderDate: Date?
    public var webInitialContentLoadResultJson: String?

    public init(
        bookmarkId: String? = nil,
        inlineImageDocumentSrc: String? = nil,
        tocJson: String? = nil,
        pendingTocNavigationId: String? = nil,
        pendingTocNavigationExtrasJson: String? = nil,
        lastExportMarkdown: String? = nil,
        lastExportPdfBase64: String? = nil,
        reminderDate: Date? = nil,
        webInitialContentLoadResultJson: String? = nil
    ) {
        self.bookmarkId = bookmarkId
        self.inlineImageDocumentSrc = inlineImageDocumentSrc
        self.tocJson = tocJson
        self.pendingTocNavigationId = pendingTocNavigationId
        self.pendingTocNavigationExtrasJson = pendingTocNavigationExtrasJson
        self.lastExportMarkdown = lastExportMarkdown
        self.lastExportPdfBase64 = lastExportPdfBase64
        self.reminderDate = reminderDate
        self.webInitialContentLoadResultJson = webInitialContentLoadResultJson
    }
}
