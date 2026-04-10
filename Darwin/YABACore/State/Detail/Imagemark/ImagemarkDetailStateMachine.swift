//
//  ImagemarkDetailStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class ImagemarkDetailStateMachine: YabaBaseObservableState<ImagemarkDetailUIState>, YabaScreenStateMachine {
    public override init(initialState: ImagemarkDetailUIState = ImagemarkDetailUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: ImagemarkDetailEvent) async {
        switch event {
        case let .onInit(bookmarkId):
            apply { $0.bookmarkId = bookmarkId }
        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])
        case .onShareImage, .onExportImage, .onRequestNotificationPermission,
             .onScheduleReminder, .onCancelReminder:
            break
        case let .updateSummary(bookmarkId, summary):
            ImagemarkManager.queueCreateOrUpdateImageDetails(bookmarkId: bookmarkId, summary: summary)
        }
    }
}
