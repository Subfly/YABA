//
//  ImagemarkDetailUIState.swift
//  YABACore
//

import Foundation

public struct ImagemarkDetailUIState: Sendable {
    public var bookmarkId: String?
    public var reminderDate: Date?

    public init(bookmarkId: String? = nil, reminderDate: Date? = nil) {
        self.bookmarkId = bookmarkId
        self.reminderDate = reminderDate
    }
}
