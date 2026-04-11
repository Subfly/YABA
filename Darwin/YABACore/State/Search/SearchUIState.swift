//
//  SearchUIState.swift
//  YABACore
//

import Foundation

public struct SearchUIState: Sendable {
    public var query: String
    public var enabledFolderFilterIds: Set<String>
    public var enabledTagFilterIds: Set<String>
    public var sortType: SortType
    public var sortOrder: SortOrderType
    public var bookmarkAppearance: BookmarkAppearance
    public var cardImageSizing: CardImageSizing?

    public init(
        query: String = "",
        enabledFolderFilterIds: Set<String> = [],
        enabledTagFilterIds: Set<String> = [],
        sortType: SortType = .editedAt,
        sortOrder: SortOrderType = .descending,
        bookmarkAppearance: BookmarkAppearance = .list,
        cardImageSizing: CardImageSizing? = .small
    ) {
        self.query = query
        self.enabledFolderFilterIds = enabledFolderFilterIds
        self.enabledTagFilterIds = enabledTagFilterIds
        self.sortType = sortType
        self.sortOrder = sortOrder
        self.bookmarkAppearance = bookmarkAppearance
        self.cardImageSizing = cardImageSizing
    }
}
