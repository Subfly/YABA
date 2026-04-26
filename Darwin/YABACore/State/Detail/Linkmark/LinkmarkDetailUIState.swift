//
//  LinkmarkDetailUIState.swift
//  YABACore
//

import Foundation

public struct LinkmarkDetailUIState: Sendable {
    public var bookmarkId: String?
    /// Resolved link URL (set via [LinkmarkDetailEvent.onLinkSourceUrl]).
    public var linkSourceUrl: String?
    public var readerTheme: ReaderTheme
    public var readerFontSize: ReaderFontSize
    public var readerLineHeight: ReaderLineHeight
    public var scrollToAnnotationId: String?
    public var tocJson: String?
    public var pendingTocNavigationId: String?
    public var pendingTocNavigationExtrasJson: String?
    public var lastExportMarkdown: String?
    public var lastExportPdfBase64: String?
    public var reminderDate: Date?
    public var hasNotificationPermission: Bool
    public var readerWebInitialLoadResultJson: String?

    public init(
        bookmarkId: String? = nil,
        linkSourceUrl: String? = nil,
        readerTheme: ReaderTheme = .system,
        readerFontSize: ReaderFontSize = .medium,
        readerLineHeight: ReaderLineHeight = .normal,
        scrollToAnnotationId: String? = nil,
        tocJson: String? = nil,
        pendingTocNavigationId: String? = nil,
        pendingTocNavigationExtrasJson: String? = nil,
        lastExportMarkdown: String? = nil,
        lastExportPdfBase64: String? = nil,
        reminderDate: Date? = nil,
        hasNotificationPermission: Bool = false,
        readerWebInitialLoadResultJson: String? = nil
    ) {
        self.bookmarkId = bookmarkId
        self.linkSourceUrl = linkSourceUrl
        self.readerTheme = readerTheme
        self.readerFontSize = readerFontSize
        self.readerLineHeight = readerLineHeight
        self.scrollToAnnotationId = scrollToAnnotationId
        self.tocJson = tocJson
        self.pendingTocNavigationId = pendingTocNavigationId
        self.pendingTocNavigationExtrasJson = pendingTocNavigationExtrasJson
        self.lastExportMarkdown = lastExportMarkdown
        self.lastExportPdfBase64 = lastExportPdfBase64
        self.reminderDate = reminderDate
        self.hasNotificationPermission = hasNotificationPermission
        self.readerWebInitialLoadResultJson = readerWebInitialLoadResultJson
    }
}
