//
//  ImagemarkDetailUIState.swift
//  YABACore
//

import Foundation

public struct ImagemarkDetailUIState: Sendable {
    public var bookmarkId: String?

    public init(bookmarkId: String? = nil) {
        self.bookmarkId = bookmarkId
    }
}
