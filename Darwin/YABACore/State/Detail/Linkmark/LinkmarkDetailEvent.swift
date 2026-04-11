//
//  LinkmarkDetailEvent.swift
//  YABACore
//
//  Parity with Compose `LinkmarkDetailEvent` — complex payloads use `Data`/`String` for host bridges.
//  Reminder `titleKey` / `messageKey` are iOS `Localizable.strings` keys (resolved in `ReminderManager`).
//

import Foundation

public enum LinkmarkDetailEvent: Sendable {
    case onInit(bookmarkId: String)
    /// Call when the UI has resolved the link URL (needed for refresh).
    case onLinkSourceUrl(String)
    case onSaveReadableContent(documentJson: Data)
    case onUpdateReadableRequested
    case onUpdateLinkMetadataRequested
    case onConverterSucceeded(YabaWebConverterResult)
    case onConverterFailed(errorMessage: String)
    case onReaderWebInitialContentLoad(resultJson: String?)
    case onSelectReadableVersion(versionId: String)
    case onDeleteReadableVersion(versionId: String)
    case onDeleteBookmark(bookmarkId: String)
    case onToggleReaderTheme
    case onToggleReaderFontSize
    case onToggleReaderLineHeight
    case onSetReaderTheme(YabaCoreReaderTheme)
    case onSetReaderFontSize(YabaCoreReaderFontSize)
    case onSetReaderLineHeight(YabaCoreReaderLineHeight)
    case onCreateAnnotation(
        annotationId: String,
        readableVersionId: String,
        colorRole: YabaCoreColorRole,
        note: String?,
        quoteText: String?
    )
    case onUpdateAnnotation(annotationId: String, colorRole: YabaCoreColorRole, note: String?)
    case onDeleteAnnotation(annotationId: String)
    case onAnnotationReadableCreateCommitted(annotationId: String, documentJson: String)
    case onAnnotationReadableDeleteCommitted(annotationId: String, documentJson: String)
    case onScrollToAnnotation(annotationId: String)
    case onClearScrollToAnnotation
    case onTocChanged(tocJson: String?)
    case onNavigateToTocItem(id: String, extrasJson: String?)
    case onClearTocNavigation
    case onRequestNotificationPermission
    case onScheduleReminder(fireAt: Date, titleKey: String, messageKey: String)
    case onCancelReminder
    case onExportMarkdownReady(String)
    case onExportPdfReady(base64: String)

    case updateLinkMetadata(
        bookmarkId: String,
        url: String,
        domain: String?,
        videoUrl: String?,
        audioUrl: String?,
        metadataTitle: String?,
        metadataDescription: String?,
        metadataAuthor: String?,
        metadataDate: String?
    )
}
