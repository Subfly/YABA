//
//  SearchEvent.swift
//  YABACore
//
//  Parity with Compose `SearchEvent`.
//

import Foundation

public enum SearchEvent: Sendable {
    case onInit
    case onChangeQuery(String)
    case onToggleFolderFilter(folderId: String)
    case onToggleTagFilter(tagId: String)
    case onChangeSort(sortType: SortType, sortOrder: SortOrderType)
    case onChangeAppearance(appearance: BookmarkAppearance, cardImageSizing: CardImageSizing?)
    case onDeleteBookmark(bookmarkId: String)
}
