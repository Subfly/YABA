//
//  HomeStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class HomeStateMachine: YabaBaseObservableState<HomeUIState>, YabaScreenStateMachine {
    public override init(initialState: HomeUIState = HomeUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: HomeEvent) async {
        switch event {
        case .onInit:
            apply { $0.isLoading = false }

        case let .onChangeBookmarkAppearance(a):
            apply { $0.bookmarkAppearance = a }
        case let .onChangeCardImageSizing(s):
            apply { $0.cardImageSizing = s }
        case let .onChangeCollectionSorting(t):
            apply { $0.collectionSortType = t }
        case let .onChangeSortOrder(o):
            apply { $0.sortOrder = o }

        case let .onToggleFolderExpanded(folderId):
            apply {
                if $0.expandedFolderIds.contains(folderId) {
                    $0.expandedFolderIds.remove(folderId)
                } else {
                    $0.expandedFolderIds.insert(folderId)
                }
            }

        case let .onDeleteFolder(folderId):
            FolderManager.queueDeleteFolder(folderId: folderId)

        case let .onMoveFolder(folderId, newParent):
            FolderManager.queueMoveFolder(folderId: folderId, newParentFolderId: newParent)

        case let .onDeleteTag(tagId):
            TagManager.queueDeleteOrHideTag(tagId: tagId)

        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])

        case let .onMoveBookmarkToFolder(bookmarkId, targetFolderId):
            AllBookmarksManager.queueMoveBookmarksToFolder(bookmarkIds: [bookmarkId], targetFolderId: targetFolderId)

        case let .onMoveBookmarkToTag(bookmarkId, targetTagId):
            AllBookmarksManager.queueAddTagToBookmark(tagId: targetTagId, bookmarkId: bookmarkId)
        }
    }
}
