//
//  BookmarkSelectionUIState.swift
//  YABACore
//

import Foundation

public struct BookmarkSelectionUIState: Sendable {
    public var selectedBookmarkId: String?
    public var query: String

    public init(selectedBookmarkId: String? = nil, query: String = "") {
        self.selectedBookmarkId = selectedBookmarkId
        self.query = query
    }
}
