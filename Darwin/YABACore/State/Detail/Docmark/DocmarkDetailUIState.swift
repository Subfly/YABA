//
//  DocmarkDetailUIState.swift
//  YABACore
//

import Foundation

public struct DocmarkDetailUIState: Sendable {
    public var bookmarkId: String?
    public var readerTheme: YabaCoreReaderTheme
    public var readerFontSize: YabaCoreReaderFontSize
    public var readerLineHeight: YabaCoreReaderLineHeight
    public var scrollToAnnotationId: String?
    public var tocJson: String?
    public var pendingTocNavigationId: String?
    public var pendingTocNavigationExtrasJson: String?
    public var reminderDate: Date?
    public var webInitialContentLoadResultJson: String?

    public init(
        bookmarkId: String? = nil,
        readerTheme: YabaCoreReaderTheme = .system,
        readerFontSize: YabaCoreReaderFontSize = .medium,
        readerLineHeight: YabaCoreReaderLineHeight = .normal,
        scrollToAnnotationId: String? = nil,
        tocJson: String? = nil,
        pendingTocNavigationId: String? = nil,
        pendingTocNavigationExtrasJson: String? = nil,
        reminderDate: Date? = nil,
        webInitialContentLoadResultJson: String? = nil
    ) {
        self.bookmarkId = bookmarkId
        self.readerTheme = readerTheme
        self.readerFontSize = readerFontSize
        self.readerLineHeight = readerLineHeight
        self.scrollToAnnotationId = scrollToAnnotationId
        self.tocJson = tocJson
        self.pendingTocNavigationId = pendingTocNavigationId
        self.pendingTocNavigationExtrasJson = pendingTocNavigationExtrasJson
        self.reminderDate = reminderDate
        self.webInitialContentLoadResultJson = webInitialContentLoadResultJson
    }
}
