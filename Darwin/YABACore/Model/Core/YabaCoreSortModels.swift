//
//  YabaCoreSortModels.swift
//  YABACore
//
//  Parity with Compose `SortType` / `SortOrderType` / `BookmarkAppearance` / `CardImageSizing`.
//

import Foundation

public enum YabaCoreSortType: String, Sendable, CaseIterable {
    case createdAt
    case editedAt
    case label
}

public enum YabaCoreSortOrderType: String, Sendable, CaseIterable {
    case ascending
    case descending
}

public enum YabaCoreBookmarkAppearance: String, Sendable, CaseIterable {
    case list
    case card
    case grid
}

public enum YabaCoreCardImageSizing: String, Sendable, CaseIterable {
    case big
    case small
}
