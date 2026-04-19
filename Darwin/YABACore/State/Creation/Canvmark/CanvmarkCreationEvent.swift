//
//  CanvmarkCreationEvent.swift
//  YABACore
//

import Foundation

public enum CanvmarkCreationEvent: Sendable {
    case onInit(
        canvmarkId: String?,
        initialFolderId: String?,
        initialTagIds: [String]?,
        uncategorizedFolderCreationRequired: Bool
    )
    case onCyclePreviewAppearance
    case onChangeLabel(String)
    case onChangeDescription(String)
    case onSelectFolderId(String?)
    case onSelectTagIds([String])
    case onSave
    case onTogglePinned

    case create(
        bookmarkId: String,
        folderId: String,
        label: String,
        bookmarkDescription: String?,
        isPinned: Bool,
        tagIds: [String]
    )
    case bootstrapCanvas(bookmarkId: String)
}
