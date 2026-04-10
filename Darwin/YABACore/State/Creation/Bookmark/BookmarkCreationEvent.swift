//
//  BookmarkCreationEvent.swift
//  YABACore
//
//  Link bookmark creation from the product “bookmark creation” route. Compose keeps kind-specific
//  machines under `creation/linkmark/`; this Darwin type is the link entry used by shared flows.
//

import Foundation

public enum BookmarkCreationEvent: Sendable {
    case createLinkBookmark(
        folderId: String,
        url: String,
        label: String,
        bookmarkDescription: String?,
        tagIds: [String],
        isPrivate: Bool,
        isPinned: Bool
    )
}
