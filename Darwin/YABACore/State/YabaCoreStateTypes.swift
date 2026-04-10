//
//  YabaCoreStateTypes.swift
//  YABACore
//
//  Sendable mirrors of Compose model utils used by state-machine events (parity with KMP enums).
//

import Foundation

// MARK: - Folder selection

public enum YabaCoreFolderSelectionMode: String, Sendable, CaseIterable {
    case folderSelection
    case parentSelection
    case folderMove
    case bookmarksMove
}

// MARK: - Sorting & appearance (Compose SortType / SortOrderType / BookmarkAppearance / CardImageSizing)

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

// MARK: - Reader (Compose ReaderTheme / ReaderFontSize / ReaderLineHeight)

public enum YabaCoreReaderTheme: String, Sendable, CaseIterable {
    case system
    case dark
    case light
    case sepia
}

public enum YabaCoreReaderFontSize: String, Sendable, CaseIterable {
    case small
    case medium
    case large
}

public enum YabaCoreReaderLineHeight: String, Sendable, CaseIterable {
    case normal
    case relaxed
}

// MARK: - Annotation color roles (Compose YabaColor.code)

public enum YabaCoreColorRole: Int, Sendable, CaseIterable {
    case none = 0
    case blue = 1
    case brown = 2
    case cyan = 3
    case gray = 4
    case green = 5
    case indigo = 6
    case mint = 7
    case orange = 8
    case pink = 9
    case purple = 10
    case red = 11
    case teal = 12
    case yellow = 13
}

// MARK: - Annotation draft (contract-only; host fills from WebView / PDF / EPUB)

public struct YabaReadableSelectionDraft: Sendable {
    public var bookmarkId: String
    public var readableVersionId: String
    public var quoteText: String?
    /// Optional JSON for anchors (PDF offsets, EPUB CFI, etc.).
    public var extrasJson: String?

    public init(bookmarkId: String, readableVersionId: String, quoteText: String? = nil, extrasJson: String? = nil) {
        self.bookmarkId = bookmarkId
        self.readableVersionId = readableVersionId
        self.quoteText = quoteText
        self.extrasJson = extrasJson
    }
}
