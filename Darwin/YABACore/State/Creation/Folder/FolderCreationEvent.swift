//
//  FolderCreationEvent.swift
//  YABACore
//
//  Parity with Compose `FolderCreationEvent`.
//

import Foundation

public enum FolderCreationEvent: Sendable {
    case onInitWithFolder(folderId: String?)
    case onSelectNewParent(parentFolderId: String?)
    case onSelectNewColor(YabaColor)
    case onSelectNewIcon(String)
    case onChangeLabel(String)
    case onChangeDescription(String)
    case onSave

    /// One-shot create (legacy helper).
    case create(folderId: String, label: String, description: String?, parentFolderId: String?)
}
