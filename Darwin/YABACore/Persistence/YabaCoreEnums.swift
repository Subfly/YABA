//
//  YabaCoreEnums.swift
//  YABACore
//
//  Mirrors Compose KMP enums for persisted raw values.
//

import Foundation

/// Matches [BookmarkKind] in Compose (`code` 0…4).
enum YabaCoreBookmarkKind: Int, Codable, CaseIterable, Sendable {
    case link = 0
    case note = 1
    case image = 2
    case file = 3
    case canvas = 4
}

/// Matches [DocmarkType] in Compose.
enum YabaCoreDocmarkType: String, Codable, CaseIterable, Sendable {
    case pdf = "PDF"
    case epub = "EPUB"
}

/// Matches [AnnotationType] in Compose.
enum YabaCoreAnnotationType: String, Codable, CaseIterable, Sendable {
    case readable = "READABLE"
    case pdf = "PDF"
    case epub = "EPUB"
}
