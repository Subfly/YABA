//
//  FolderSelectionStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class FolderSelectionStateMachine: YabaBaseObservableState<FolderSelectionUIState>, YabaScreenStateMachine {
    public override init(initialState: FolderSelectionUIState = FolderSelectionUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: FolderSelectionEvent) async {
        switch event {
        case let .onInit(mode, contextFolderId, contextBookmarkIds):
            apply {
                $0.mode = mode
                $0.contextFolderId = contextFolderId
                $0.contextBookmarkIds = contextBookmarkIds
            }
        case let .onSearchQueryChanged(q):
            apply { $0.searchQuery = q }
        case let .onMoveFolderToSelected(targetFolderId):
            guard let folderId = state.contextFolderId else { return }
            FolderManager.queueMoveFolder(folderId: folderId, newParentFolderId: targetFolderId)
        case let .onMoveBookmarksToSelected(targetFolderId):
            let ids = state.contextBookmarkIds ?? []
            guard !ids.isEmpty else { return }
            AllBookmarksManager.queueMoveBookmarksToFolder(bookmarkIds: ids, targetFolderId: targetFolderId)
        }
    }
}
