//
//  TagDetailStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class TagDetailStateMachine: YabaBaseObservableState<TagDetailUIState>, YabaScreenStateMachine {
    public override init(initialState: TagDetailUIState = TagDetailUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: TagDetailEvent) async {
        switch event {
        case let .onInit(tagId):
            apply { $0.tagId = tagId }
        case let .onChangeQuery(q):
            apply { $0.query = q }
        case .onToggleSelectionMode:
            apply {
                $0.selectionMode.toggle()
                if !$0.selectionMode { $0.selectedBookmarkIds = [] }
            }
        case let .onToggleBookmarkSelection(bookmarkId):
            apply {
                if $0.selectedBookmarkIds.contains(bookmarkId) {
                    $0.selectedBookmarkIds.remove(bookmarkId)
                } else {
                    $0.selectedBookmarkIds.insert(bookmarkId)
                }
            }
        case let .onDeleteSelected(ids):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: ids)
            apply { $0.selectedBookmarkIds = [] }
        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])
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
        case let .deleteOrHide(id):
            TagManager.queueDeleteOrHideTag(tagId: id)
        }
    }
}
