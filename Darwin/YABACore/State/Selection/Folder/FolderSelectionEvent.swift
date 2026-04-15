//
//  FolderSelectionEvent.swift
//  YABACore
//
//  Parity with Compose `FolderSelectionEvent`.
//

import Foundation

public enum FolderSelectionEvent: Sendable {
    case onInit(
        mode: FolderSelectionMode,
        contextFolderId: String?,
        contextBookmarkIds: [String]?
    )
    case onSearchQueryChanged(String)
    case onMoveFolderToSelected(targetFolderId: String?)
    case onMoveBookmarksToSelected(targetFolderId: String)
}
