//
//  HomeUIState.swift
//  YABACore
//

import Foundation

public struct HomeUIState: Sendable {
    public var isLoading: Bool
    public var bookmarkAppearance: YabaCoreBookmarkAppearance
    public var cardImageSizing: YabaCoreCardImageSizing
    public var collectionSortType: YabaCoreSortType
    public var sortOrder: YabaCoreSortOrderType
    /// Folder rows expanded in the tree UI.
    public var expandedFolderIds: Set<String>

    public init(
        isLoading: Bool = true,
        bookmarkAppearance: YabaCoreBookmarkAppearance = .list,
        cardImageSizing: YabaCoreCardImageSizing = .small,
        collectionSortType: YabaCoreSortType = .editedAt,
        sortOrder: YabaCoreSortOrderType = .descending,
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
