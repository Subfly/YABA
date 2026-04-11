//
//  HomeUIState.swift
//  YABACore
//

import Foundation

public struct HomeUIState: Sendable {
    public var isLoading: Bool
    public var bookmarkAppearance: BookmarkAppearance
    public var cardImageSizing: CardImageSizing
    public var collectionSortType: SortType
    public var sortOrder: SortOrderType
    /// Folder rows expanded in the tree UI.
    public var expandedFolderIds: Set<String>

    public init(
        isLoading: Bool = true,
        bookmarkAppearance: BookmarkAppearance = .list,
        cardImageSizing: CardImageSizing = .small,
        collectionSortType: SortType = .editedAt,
        sortOrder: SortOrderType = .descending,
        expandedFolderIds: Set<String> = []
    ) {
        self.isLoading = isLoading
        self.bookmarkAppearance = bookmarkAppearance
        self.cardImageSizing = cardImageSizing
        self.collectionSortType = collectionSortType
        self.sortOrder = sortOrder
        self.expandedFolderIds = expandedFolderIds
    }
}
