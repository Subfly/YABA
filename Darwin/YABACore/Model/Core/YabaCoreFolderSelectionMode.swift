//
//  YabaCoreFolderSelectionMode.swift
//  YABACore
//
//  Parity with Compose `FolderSelectionMode`.
//

import Foundation

public enum YabaCoreFolderSelectionMode: String, Sendable, CaseIterable {
    case folderSelection
    case parentSelection
    case folderMove
    case bookmarksMove
}
