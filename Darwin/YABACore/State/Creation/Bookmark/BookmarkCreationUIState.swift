//
//  BookmarkCreationUIState.swift
//  YABACore
//

import Foundation

public struct BookmarkCreationUIState: Sendable {
    public var isSaving: Bool = false
    public var lastError: String?

    public init(isSaving: Bool = false, lastError: String? = nil) {
        self.isSaving = isSaving
        self.lastError = lastError
    }
}
