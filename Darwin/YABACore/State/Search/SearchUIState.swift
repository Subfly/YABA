//
//  SearchUIState.swift
//  YABACore
//

import Foundation

public struct SearchUIState: Sendable {
    public var query: String
    public var enabledFolderFilterIds: Set<String>
    public var enabledTagFilterIds: Set<String>
    public var sortType: YabaCoreSortType
    public var sortOrder: YabaCoreSortOrderType
    public var bookmarkAppearance: YabaCoreBookmarkAppearance
    public var cardImageSizing: YabaCoreCardImageSizing?

    public init(
        query: String = "",
        enabledFolderFilterIds: Set<String> = [],
        enabledTagFilterIds: Set<String> = [],
        sortType: YabaCoreSortType = .editedAt,
        sortOrder: YabaCoreSortOrderType = .descending,
        bookmarkAppearance: YabaCoreBookmarkAppearance = .list,
        cardImageSizing: YabaCoreCardImageSizing? = .small
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
