//
//  PrivateBookmarkPasswordEvents.swift
//  YABACore
//
//  Compose `PrivateBookmarkPasswordReason` / `PrivateBookmarkPasswordEntryResult` parity.
//

import Foundation

public enum PrivateBookmarkPasswordReason: String, Sendable, Equatable {
    case unlockSession = "UNLOCK_SESSION"
    case openBookmark = "OPEN_BOOKMARK"
    case togglePrivateOn = "TOGGLE_PRIVATE_ON"
    case togglePrivateOff = "TOGGLE_PRIVATE_OFF"
    case editBookmark = "EDIT_BOOKMARK"
    case shareBookmark = "SHARE_BOOKMARK"
    case deleteBookmark = "DELETE_BOOKMARK"
}

public struct PrivateBookmarkPasswordEntryResult: Sendable, Equatable {
    public let bookmarkId: String?
    public let reason: PrivateBookmarkPasswordReason

    public init(bookmarkId: String?, reason: PrivateBookmarkPasswordReason) {
        self.bookmarkId = bookmarkId
        self.reason = reason
    }
}

/// Single-handler bus for follow-up actions after a successful PIN entry (Compose `PrivateBookmarkPasswordEventBus`).
@MainActor
public enum PrivateBookmarkPasswordEventBus {
    public static var onEntrySucceeded: ((PrivateBookmarkPasswordEntryResult) -> Void)?

    public static func emit(_ result: PrivateBookmarkPasswordEntryResult) {
        onEntrySucceeded?(result)
    }
}
