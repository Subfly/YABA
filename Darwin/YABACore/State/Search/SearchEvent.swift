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
    case onChangeSort(sortType: YabaCoreSortType, sortOrder: YabaCoreSortOrderType)
    case onChangeAppearance(appearance: YabaCoreBookmarkAppearance, cardImageSizing: YabaCoreCardImageSizing?)
    case onDeleteBookmark(bookmarkId: String)
}
