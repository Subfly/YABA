//
//  BookmarkSelectionEvent.swift
//  YABACore
//
//  Parity with Compose `BookmarkSelectionEvent`.
//

import Foundation

public enum BookmarkSelectionEvent: Sendable {
    case onInit(selectedBookmarkId: String?)
    case onChangeQuery(String)
    case onSelectBookmark(bookmarkId: String)
    /// Bulk delete for multi-select flows (Darwin extension; Compose uses detail flows for delete).
    case delete(bookmarkIds: [String])
    case moveToFolder(bookmarkIds: [String], folderId: String)
}
