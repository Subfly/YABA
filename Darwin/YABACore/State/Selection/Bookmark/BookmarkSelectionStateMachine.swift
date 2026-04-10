//
//  BookmarkSelectionStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class BookmarkSelectionStateMachine: YabaBaseObservableState<BookmarkSelectionUIState>, YabaScreenStateMachine {
    public override init(initialState: BookmarkSelectionUIState = BookmarkSelectionUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: BookmarkSelectionEvent) async {
        switch event {
        case let .onInit(selectedBookmarkId):
            apply { $0.selectedBookmarkId = selectedBookmarkId }
        case let .onChangeQuery(q):
            apply { $0.query = q }
        case let .onSelectBookmark(bookmarkId):
            apply { $0.selectedBookmarkId = bookmarkId }
        case let .delete(ids):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: ids)
        case let .moveToFolder(ids, folderId):
            AllBookmarksManager.queueMoveBookmarksToFolder(bookmarkIds: ids, targetFolderId: folderId)
        }
    }
}
