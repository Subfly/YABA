//
//  FolderCreationUIState.swift
//  YABACore
//

import Foundation

public struct FolderCreationUIState: Sendable {
    public var existingFolderId: String?
    public var parentFolderId: String?
    public var label: String
    public var folderDescription: String
    public var icon: String
    public var colorRole: YabaCoreColorRole
    public var lastError: String?

    public init(
        existingFolderId: String? = nil,
        parentFolderId: String? = nil,
        label: String = "",
        folderDescription: String = "",
        icon: String = "folder-01",
        colorRole: YabaCoreColorRole = .none,
        lastError: String? = nil
    ) {
        self.existingFolderId = existingFolderId
        self.parentFolderId = parentFolderId
        self.label = label
        self.folderDescription = folderDescription
        self.icon = icon
        self.colorRole = colorRole
        self.lastError = lastError
    }
}
