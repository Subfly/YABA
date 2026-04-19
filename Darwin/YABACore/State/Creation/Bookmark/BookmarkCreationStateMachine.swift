//
//  BookmarkCreationStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class BookmarkCreationStateMachine: YabaBaseObservableState<BookmarkCreationUIState>, YabaScreenStateMachine {
    public override init(initialState: BookmarkCreationUIState = BookmarkCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: BookmarkCreationEvent) async {
        switch event {
        case let .createLinkBookmark(
            folderId,
            url,
            label,
            bookmarkDescription,
            tagIds,
            isPinned
        ):
            apply { $0.isSaving = true; $0.lastError = nil }
            let bookmarkId = UUID().uuidString
            AllBookmarksManager.queueCreateBookmark(
                bookmarkId: bookmarkId,
                folderId: folderId,
                kind: .link,
                label: label,
                bookmarkDescription: bookmarkDescription,
                isPinned: isPinned,
                tagIds: tagIds
            )
            LinkmarkManager.queueCreateOrUpdateLinkDetails(
                bookmarkId: bookmarkId,
                url: url,
                domain: nil,
                videoUrl: nil,
                audioUrl: nil,
                metadataTitle: nil,
                metadataDescription: nil,
                metadataAuthor: nil,
                metadataDate: nil
            )
            apply { $0.isSaving = false }
        }
    }
}
