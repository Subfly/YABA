//
//  CanvmarkCreationEvent.swift
//  YABACore
//

import Foundation

public enum CanvmarkCreationEvent: Sendable {
    case onInit(canvmarkId: String?, initialFolderId: String?, initialTagIds: [String]?)
    case onCyclePreviewAppearance
    case onChangeLabel(String)
    case onChangeDescription(String)
    case onSelectFolderId(String?)
    case onSelectTagIds([String])
    case onSave
    case onTogglePrivate
    case onTogglePinned

    case create(
        bookmarkId: String,
        folderId: String,
        label: String,
        bookmarkDescription: String?,
        isPrivate: Bool,
        isPinned: Bool,
        tagIds: [String]
    )
    case bootstrapCanvas(bookmarkId: String)
}
