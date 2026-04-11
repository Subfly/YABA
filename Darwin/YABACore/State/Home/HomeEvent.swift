//
//  HomeEvent.swift
//  YABACore
//
//  Parity with Compose `HomeEvent` — IDs only; list rows come from `@Query`.
//

import Foundation

public enum HomeEvent: Sendable {
    case onInit

    // Preferences (host may mirror to `UserDefaults` / `@AppStorage`)
    case onChangeBookmarkAppearance(BookmarkAppearance)
    case onChangeCardImageSizing(CardImageSizing)
    case onChangeCollectionSorting(SortType)
    case onChangeSortOrder(SortOrderType)

    case onToggleFolderExpanded(folderId: String)
    case onDeleteFolder(folderId: String)
    case onMoveFolder(folderId: String, newParentFolderId: String?)

    case onDeleteTag(tagId: String)

    case onDeleteBookmark(bookmarkId: String)
    case onMoveBookmarkToFolder(bookmarkId: String, targetFolderId: String)
    /// Same as Compose `OnMoveBookmarkToTag` — adds `targetTag` association.
    case onMoveBookmarkToTag(bookmarkId: String, targetTagId: String)
}
