//
//  TagDetailUIState.swift
//  YABACore
//

import Foundation

public struct TagDetailUIState: Sendable {
    public var tagId: String?
    public var query: String
    public var selectionMode: Bool
    public var selectedBookmarkIds: Set<String>
    public var sortType: YabaCoreSortType
    public var sortOrder: YabaCoreSortOrderType
    public var bookmarkAppearance: YabaCoreBookmarkAppearance
    public var cardImageSizing: YabaCoreCardImageSizing?

    public init(
        tagId: String? = nil,
        query: String = "",
        selectionMode: Bool = false,
        selectedBookmarkIds: Set<String> = [],
        sortType: YabaCoreSortType = .editedAt,
        sortOrder: YabaCoreSortOrderType = .descending,
        bookmarkAppearance: YabaCoreBookmarkAppearance = .list,
        cardImageSizing: YabaCoreCardImageSizing? = .small
    ) {
        self.tagId = tagId
        self.query = query
        self.selectionMode = selectionMode
        self.selectedBookmarkIds = selectedBookmarkIds
        self.sortType = sortType
        self.sortOrder = sortOrder
        self.bookmarkAppearance = bookmarkAppearance
        self.cardImageSizing = cardImageSizing
    }
}
