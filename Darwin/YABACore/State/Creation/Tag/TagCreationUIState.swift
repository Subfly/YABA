//
//  TagCreationUIState.swift
//  YABACore
//

import Foundation

public struct TagCreationUIState: Sendable {
    public var existingTagId: String?
    public var label: String
    public var icon: String
    public var colorRole: YabaCoreColorRole
    public var lastError: String?

    public init(
        existingTagId: String? = nil,
        label: String = "",
        icon: String = "tag-01",
        colorRole: YabaCoreColorRole = .none,
        lastError: String? = nil
    ) {
        self.existingTagId = existingTagId
        self.label = label
        self.icon = icon
        self.colorRole = colorRole
        self.lastError = lastError
    }
}
