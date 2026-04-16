//
//  BookmarkFlowContext.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import Foundation

struct BookmarkFlowContext: Identifiable, Equatable {
    let id: UUID
    let editingBookmarkId: String?
    let preselectedFolderId: String?
    let preselectedTagIds: [String]
    let initialLinkURL: String?

    static func edit(bookmarkId: String, id: UUID = UUID()) -> BookmarkFlowContext {
        BookmarkFlowContext(
            id: id,
            editingBookmarkId: bookmarkId,
            preselectedFolderId: nil,
            preselectedTagIds: [],
            initialLinkURL: nil
        )
    }

    static func deepLink(url: String, id: UUID = UUID()) -> BookmarkFlowContext {
        BookmarkFlowContext(
            id: id,
            editingBookmarkId: nil,
            preselectedFolderId: nil,
            preselectedTagIds: [],
            initialLinkURL: url
        )
    }
}
