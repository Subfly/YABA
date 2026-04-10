//
//  FolderDetailStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class FolderDetailStateMachine: YabaBaseObservableState<FolderDetailUIState>, YabaScreenStateMachine {
    public override init(initialState: FolderDetailUIState = FolderDetailUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: FolderDetailEvent) async {
        switch event {
        case let .onInit(folderId):
            apply { $0.folderId = folderId }
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

        case let .updateMetadata(id, label, description, icon, color):
            FolderManager.queueUpdateFolderMetadata(
                folderId: id,
                label: label,
                folderDescription: description,
                icon: icon,
                colorRaw: color
            )
        case let .move(id, parent):
            FolderManager.queueMoveFolder(folderId: id, newParentFolderId: parent)
        case let .delete(id):
            FolderManager.queueDeleteFolder(folderId: id)
        }
    }
}
