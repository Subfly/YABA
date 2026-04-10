//
//  DocmarkDetailEvent.swift
//  YABACore
//

import Foundation

public enum DocmarkDetailEvent: Sendable {
    case onInit(bookmarkId: String)
    case onDeleteBookmark(bookmarkId: String)
    case onShareDocument
    case onExportDocument
    case onToggleReaderTheme
    case onToggleReaderFontSize
    case onToggleReaderLineHeight
    case onSetReaderTheme(YabaCoreReaderTheme)
    case onSetReaderFontSize(YabaCoreReaderFontSize)
    case onSetReaderLineHeight(YabaCoreReaderLineHeight)
    case onDeleteAnnotation(annotationId: String)
    case onScrollToAnnotation(annotationId: String)
    case onClearScrollToAnnotation
    case onTocChanged(tocJson: String?)
    case onNavigateToTocItem(id: String, extrasJson: String?)
    case onClearTocNavigation
    case onRequestNotificationPermission
    case onScheduleReminder(title: String, message: String, triggerAtEpochMillis: Int64)
    case onCancelReminder
    case onWebInitialContentLoad(resultJson: String?)

    case updateDocMetadata(bookmarkId: String, summary: String?, type: YabaCoreDocmarkType?)
}
