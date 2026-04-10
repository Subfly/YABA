//
//  DocmarkDetailStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class DocmarkDetailStateMachine: YabaBaseObservableState<DocmarkDetailUIState>, YabaScreenStateMachine {
    public override init(initialState: DocmarkDetailUIState = DocmarkDetailUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: DocmarkDetailEvent) async {
        switch event {
        case let .onInit(bookmarkId):
            apply { $0.bookmarkId = bookmarkId }
        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])
        case .onShareDocument, .onExportDocument, .onRequestNotificationPermission, .onWebInitialContentLoad:
            break
        case .onScheduleReminder:
            break
        case .onCancelReminder:
            break
        case .onToggleReaderTheme:
            apply {
                switch $0.readerTheme {
                case .system: $0.readerTheme = .light
                case .light: $0.readerTheme = .dark
                case .dark: $0.readerTheme = .sepia
                case .sepia: $0.readerTheme = .system
                }
            }
        case .onToggleReaderFontSize:
            apply {
                switch $0.readerFontSize {
                case .small: $0.readerFontSize = .medium
                case .medium: $0.readerFontSize = .large
                case .large: $0.readerFontSize = .small
                }
            }
        case .onToggleReaderLineHeight:
            apply { $0.readerLineHeight = $0.readerLineHeight == .normal ? .relaxed : .normal }
        case let .onSetReaderTheme(t):
            apply { $0.readerTheme = t }
        case let .onSetReaderFontSize(s):
            apply { $0.readerFontSize = s }
        case let .onSetReaderLineHeight(l):
            apply { $0.readerLineHeight = l }
        case let .onDeleteAnnotation(annotationId):
            AnnotationManager.queueDeleteAnnotation(annotationId: annotationId)
        case let .onScrollToAnnotation(annotationId):
            apply { $0.scrollToAnnotationId = annotationId }
        case .onClearScrollToAnnotation:
            apply { $0.scrollToAnnotationId = nil }
        case let .onTocChanged(tocJson):
            apply { $0.tocJson = tocJson }
        case let .onNavigateToTocItem(id, extrasJson):
            apply {
                $0.pendingTocNavigationId = id
                $0.pendingTocNavigationExtrasJson = extrasJson
            }
        case .onClearTocNavigation:
            apply {
                $0.pendingTocNavigationId = nil
                $0.pendingTocNavigationExtrasJson = nil
            }
        case let .updateDocMetadata(bookmarkId, summary, type):
            DocmarkManager.queueCreateOrUpdateDocDetails(bookmarkId: bookmarkId, summary: summary, docmarkType: type)
        }
    }
}
