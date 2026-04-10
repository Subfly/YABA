//
//  NotemarkCreationEvent.swift
//  YABACore
//

import Foundation

public enum NotemarkCreationEvent: Sendable {
    case onInit(notemarkId: String?, initialFolderId: String?, initialTagIds: [String]?)
    case onCyclePreviewAppearance
    case onChangeLabel(String)
    case onChangeDescription(String)
    case onSelectFolderId(String?)
    case onSelectTagIds([String])
    case onSave
    case onTogglePrivate
    case onTogglePinned

    case createBookmark(
        bookmarkId: String,
        folderId: String,
        label: String,
        bookmarkDescription: String?,
        isPrivate: Bool,
        isPinned: Bool,
        tagIds: [String]
    )
    case bootstrapNoteSubtype(bookmarkId: String)
}
