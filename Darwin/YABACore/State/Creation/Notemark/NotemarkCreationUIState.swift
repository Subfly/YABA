//
//  NotemarkCreationUIState.swift
//  YABACore
//

import Foundation

public struct NotemarkCreationUIState: Sendable {
    public var editingBookmarkId: String?
    public var label: String
    public var bookmarkDescription: String
    public var selectedFolderId: String?
    public var selectedTagIds: [String]
    public var documentJson: String
    public var bookmarkAppearance: YabaCoreBookmarkAppearance
    public var cardImageSizing: YabaCoreCardImageSizing
    public var isPrivate: Bool
    public var isPinned: Bool
    public var isSaving: Bool
    public var lastError: String?

    public init(
        editingBookmarkId: String? = nil,
        label: String = "",
        bookmarkDescription: String = "",
        selectedFolderId: String? = nil,
        selectedTagIds: [String] = [],
        documentJson: String = "",
        bookmarkAppearance: YabaCoreBookmarkAppearance = .list,
        cardImageSizing: YabaCoreCardImageSizing = .small,
        isPrivate: Bool = false,
        isPinned: Bool = false,
        isSaving: Bool = false,
        lastError: String? = nil
    ) {
        self.editingBookmarkId = editingBookmarkId
        self.label = label
        self.bookmarkDescription = bookmarkDescription
        self.selectedFolderId = selectedFolderId
        self.selectedTagIds = selectedTagIds
        self.documentJson = documentJson
        self.bookmarkAppearance = bookmarkAppearance
        self.cardImageSizing = cardImageSizing
        self.isPrivate = isPrivate
        self.isPinned = isPinned
        self.isSaving = isSaving
        self.lastError = lastError
    }
}
