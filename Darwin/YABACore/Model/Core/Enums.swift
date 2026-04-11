//
//  YabaCoreEnums.swift
//  YABACore
//
//  Mirrors Compose KMP enums for persisted/shared raw values.
//

import Foundation
import SwiftUI

/// Matches [BookmarkKind] in Compose (`code` 0…4).
public enum BookmarkKind: Int, Codable, CaseIterable, Sendable {
    case link = 0
    case note = 1
    case image = 2
    case file = 3
    case canvas = 4
}

public extension BookmarkKind {
    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .link: return LocalizedStringKey("Bookmark Type Link")
        case .note: return LocalizedStringKey("Bookmark Type None")
        case .image: return LocalizedStringKey("Bookmark Type Image")
        case .file: return LocalizedStringKey("Bookmark Type Audio")
        case .canvas: return LocalizedStringKey("Bookmark Type Video")
        }
    }

    public func getIconName() -> String {
        switch self {
        case .link: return "safari"
        case .note: return "bookmark-02"
        case .image: return "image-03"
        case .file: return "headphones"
        case .canvas: return "video-01"
        }
    }
}

/// Matches [DocmarkType] in Compose.
public enum DocmarkType: String, Codable, CaseIterable, Sendable {
    case pdf = "PDF"
    case epub = "EPUB"
}

/// Matches [AnnotationType] in Compose.
public enum AnnotationType: String, Codable, CaseIterable, Sendable {
    case readable = "READABLE"
    case pdf = "PDF"
    case epub = "EPUB"
}

/// Legacy app-side compatibility enum kept in YABACore after YABA model cleanup.
public enum BookmarkType: Int, Codable, CaseIterable, Sendable {
    case none = 1
    case webLink = 2
    case video = 3
    case image = 4
    case audio = 5
    case music = 6

    public func getIconName() -> String {
        switch self {
        case .none: return "bookmark-02"
        case .webLink: return "safari"
        case .video: return "video-01"
        case .image: return "image-03"
        case .audio: return "headphones"
        case .music: return "music-note-01"
        }
    }

    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .none: return LocalizedStringKey("Bookmark Type None")
        case .webLink: return LocalizedStringKey("Bookmark Type Link")
        case .video: return LocalizedStringKey("Bookmark Type Video")
        case .image: return LocalizedStringKey("Bookmark Type Image")
        case .audio: return LocalizedStringKey("Bookmark Type Audio")
        case .music: return LocalizedStringKey("Bookmark Type Music")
        }
    }
}

/// Legacy app-side compatibility enum kept in YABACore after YABA model cleanup.
public enum CollectionType: Int, Codable, CaseIterable, Sendable {
    case folder = 1
    case tag = 2
}
