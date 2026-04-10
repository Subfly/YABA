//
//  SearchStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class SearchStateMachine: YabaBaseObservableState<SearchUIState>, YabaScreenStateMachine {
    public override init(initialState: SearchUIState = SearchUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: SearchEvent) async {
        switch event {
        case .onInit:
            break
        case let .onChangeQuery(q):
            apply { $0.query = q }
        case let .onToggleFolderFilter(folderId):
            apply {
                if $0.enabledFolderFilterIds.contains(folderId) {
                    $0.enabledFolderFilterIds.remove(folderId)
                } else {
                    $0.enabledFolderFilterIds.insert(folderId)
                }
            }
        case let .onToggleTagFilter(tagId):
            apply {
                if $0.enabledTagFilterIds.contains(tagId) {
                    $0.enabledTagFilterIds.remove(tagId)
                } else {
                    $0.enabledTagFilterIds.insert(tagId)
                }
            }
        case let .onChangeSort(sortType, sortOrder):
            apply {
                $0.sortType = sortType
                $0.sortOrder = sortOrder
            }
        case let .onChangeAppearance(appearance, sizing):
            apply {
                $0.bookmarkAppearance = appearance
                $0.cardImageSizing = sizing
            }
        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])
        }
    }
}
