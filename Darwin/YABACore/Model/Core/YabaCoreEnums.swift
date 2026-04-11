//
//  YabaCoreEnums.swift
//  YABACore
//
//  Mirrors Compose KMP enums for persisted/shared raw values.
//

import Foundation

/// Matches [BookmarkKind] in Compose (`code` 0…4).
public enum YabaCoreBookmarkKind: Int, Codable, CaseIterable, Sendable {
    case link = 0
    case note = 1
    case image = 2
    case file = 3
    case canvas = 4
}

/// Matches [DocmarkType] in Compose.
public enum YabaCoreDocmarkType: String, Codable, CaseIterable, Sendable {
    case pdf = "PDF"
    case epub = "EPUB"
}

/// Matches [AnnotationType] in Compose.
public enum YabaCoreAnnotationType: String, Codable, CaseIterable, Sendable {
    case readable = "READABLE"
    case pdf = "PDF"
    case epub = "EPUB"
}
