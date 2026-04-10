//
//  LinkmarkDetailUIState.swift
//  YABACore
//

import Foundation

public struct LinkmarkDetailUIState: Sendable {
    public var bookmarkId: String?
    public var selectedReadableVersionId: String?
    public var readerTheme: YabaCoreReaderTheme
    public var readerFontSize: YabaCoreReaderFontSize
    public var readerLineHeight: YabaCoreReaderLineHeight
    public var scrollToAnnotationId: String?
    public var tocJson: String?
    public var pendingTocNavigationId: String?
    public var pendingTocNavigationExtrasJson: String?
    public var lastExportMarkdown: String?
    public var lastExportPdfBase64: String?
    public var pendingReminderTitle: String?

    public init(
        bookmarkId: String? = nil,
        selectedReadableVersionId: String? = nil,
        readerTheme: YabaCoreReaderTheme = .system,
        readerFontSize: YabaCoreReaderFontSize = .medium,
        readerLineHeight: YabaCoreReaderLineHeight = .normal,
        scrollToAnnotationId: String? = nil,
        tocJson: String? = nil,
        pendingTocNavigationId: String? = nil,
        pendingTocNavigationExtrasJson: String? = nil,
        lastExportMarkdown: String? = nil,
        lastExportPdfBase64: String? = nil,
        pendingReminderTitle: String? = nil
    ) {
        self.bookmarkId = bookmarkId
        self.selectedReadableVersionId = selectedReadableVersionId
        self.readerTheme = readerTheme
        self.readerFontSize = readerFontSize
        self.readerLineHeight = readerLineHeight
        self.scrollToAnnotationId = scrollToAnnotationId
        self.tocJson = tocJson
        self.pendingTocNavigationId = pendingTocNavigationId
        self.pendingTocNavigationExtrasJson = pendingTocNavigationExtrasJson
        self.lastExportMarkdown = lastExportMarkdown
        self.lastExportPdfBase64 = lastExportPdfBase64
        self.pendingReminderTitle = pendingReminderTitle
    }
}
