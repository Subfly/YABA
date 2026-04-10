//
//  FolderSelectionUIState.swift
//  YABACore
//

import Foundation

public struct FolderSelectionUIState: Sendable {
    public var mode: YabaCoreFolderSelectionMode
    public var contextFolderId: String?
    public var contextBookmarkIds: [String]?
    public var searchQuery: String

    public init(
        mode: YabaCoreFolderSelectionMode = .folderSelection,
        contextFolderId: String? = nil,
        contextBookmarkIds: [String]? = nil,
        searchQuery: String = ""
    ) {
        self.mode = mode
        self.contextFolderId = contextFolderId
        self.contextBookmarkIds = contextBookmarkIds
        self.searchQuery = searchQuery
    }
}
