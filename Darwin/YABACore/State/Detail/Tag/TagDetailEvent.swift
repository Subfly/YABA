//
//  TagDetailEvent.swift
//  YABACore
//
//  Parity with Compose `TagDetailEvent` + tag delete/hide.
//

import Foundation

public enum TagDetailEvent: Sendable {
    case onInit(tagId: String)
    case onChangeQuery(String)
    case onToggleSelectionMode
    case onToggleBookmarkSelection(bookmarkId: String)
    case onDeleteSelected(bookmarkIds: [String])
    case onDeleteBookmark(bookmarkId: String)
    case onChangeSort(sortType: YabaCoreSortType, sortOrder: YabaCoreSortOrderType)
    case onChangeAppearance(appearance: YabaCoreBookmarkAppearance, cardImageSizing: YabaCoreCardImageSizing?)

    case deleteOrHide(tagId: String)
}
