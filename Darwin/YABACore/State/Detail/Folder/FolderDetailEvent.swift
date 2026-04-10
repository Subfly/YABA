//
//  FolderDetailEvent.swift
//  YABACore
//
//  Parity with Compose `FolderDetailEvent` + folder metadata mutations.
//

import Foundation

public enum FolderDetailEvent: Sendable {
    case onInit(folderId: String)
    case onChangeQuery(String)
    case onToggleSelectionMode
    case onToggleBookmarkSelection(bookmarkId: String)
    case onDeleteSelected(bookmarkIds: [String])
    case onDeleteBookmark(bookmarkId: String)
    case onChangeSort(sortType: YabaCoreSortType, sortOrder: YabaCoreSortOrderType)
    case onChangeAppearance(appearance: YabaCoreBookmarkAppearance, cardImageSizing: YabaCoreCardImageSizing?)

    case updateMetadata(folderId: String, label: String, description: String?, icon: String, colorRaw: Int)
    case move(folderId: String, newParentId: String?)
    case delete(folderId: String)
}
