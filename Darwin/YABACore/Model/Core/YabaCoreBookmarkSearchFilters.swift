//
//  YabaCoreBookmarkSearchFilters.swift
//  YABACore
//
//  Parity with Compose `BookmarkSearchFilters`.
//

import Foundation

public struct YabaCoreBookmarkSearchFilters: Sendable, Equatable {
    public var folderIds: Set<String>?
    public var tagIds: Set<String>?

    public init(folderIds: Set<String>? = nil, tagIds: Set<String>? = nil) {
        self.folderIds = folderIds
        self.tagIds = tagIds
    }
}
