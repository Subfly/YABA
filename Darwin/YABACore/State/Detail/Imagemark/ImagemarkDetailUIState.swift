//
//  ImagemarkDetailUIState.swift
//  YABACore
//

import Foundation

public struct ImagemarkDetailUIState: Sendable {
    public var bookmarkId: String?
    public var reminderDate: Date?
    /// Populated by `onShareImage`; consume with `onConsumePendingShare` after presenting share UI.
    public var pendingShareFileURL: URL?

    public init(
        bookmarkId: String? = nil,
        reminderDate: Date? = nil,
        pendingShareFileURL: URL? = nil
    ) {
        self.bookmarkId = bookmarkId
        self.reminderDate = reminderDate
        self.pendingShareFileURL = pendingShareFileURL
    }
}
