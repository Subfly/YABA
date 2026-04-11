//
//  ImagemarkCreationUIState.swift
//  YABACore
//

import Foundation

public struct ImagemarkCreationUIState: Sendable {
    public var editingBookmarkId: String?
    public var label: String
    public var bookmarkDescription: String
    public var summary: String
    public var selectedFolderId: String?
    public var selectedTagIds: [String]
    public var imageData: Data?
    public var imageFileExtension: String
    public var bookmarkAppearance: BookmarkAppearance
    public var cardImageSizing: CardImageSizing
    public var isPrivate: Bool
    public var isPinned: Bool
    public var isSaving: Bool
    public var lastError: String?

    public init(
        editingBookmarkId: String? = nil,
        label: String = "",
        bookmarkDescription: String = "",
        summary: String = "",
        selectedFolderId: String? = nil,
        selectedTagIds: [String] = [],
        imageData: Data? = nil,
        imageFileExtension: String = "jpg",
        bookmarkAppearance: BookmarkAppearance = .list,
        cardImageSizing: CardImageSizing = .small,
        isPrivate: Bool = false,
        isPinned: Bool = false,
        isSaving: Bool = false,
        lastError: String? = nil
    ) {
        self.editingBookmarkId = editingBookmarkId
        self.label = label
        self.bookmarkDescription = bookmarkDescription
        self.summary = summary
        self.selectedFolderId = selectedFolderId
        self.selectedTagIds = selectedTagIds
        self.imageData = imageData
        self.imageFileExtension = imageFileExtension
        self.bookmarkAppearance = bookmarkAppearance
        self.cardImageSizing = cardImageSizing
        self.isPrivate = isPrivate
        self.isPinned = isPinned
        self.isSaving = isSaving
        self.lastError = lastError
    }
}
