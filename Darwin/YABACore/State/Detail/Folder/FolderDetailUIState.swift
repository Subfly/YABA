//
//  FolderDetailUIState.swift
//  YABACore
//

import Foundation

public struct FolderDetailUIState: Sendable {
    public var folderId: String?
    public var query: String
    public var selectionMode: Bool
    public var selectedBookmarkIds: Set<String>
    public var sortType: SortType
    public var sortOrder: SortOrderType
    public var bookmarkAppearance: BookmarkAppearance
    public var cardImageSizing: CardImageSizing?

    public init(
        folderId: String? = nil,
        query: String = "",
        selectionMode: Bool = false,
        selectedBookmarkIds: Set<String> = [],
        sortType: SortType = .editedAt,
        sortOrder: SortOrderType = .descending,
        bookmarkAppearance: BookmarkAppearance = .list,
        cardImageSizing: CardImageSizing? = .small
    ) {
        self.folderId = folderId
        self.query = query
        self.selectionMode = selectionMode
        self.selectedBookmarkIds = selectedBookmarkIds
        self.sortType = sortType
        self.sortOrder = sortOrder
        self.bookmarkAppearance = bookmarkAppearance
        self.cardImageSizing = cardImageSizing
    }
}
