//
//  DocmarkCreationUIState.swift
//  YABACore
//

import Foundation

public struct DocmarkCreationUIState: Sendable {
    public var editingBookmarkId: String?
    public var label: String
    public var bookmarkDescription: String
    public var summary: String
    public var selectedFolderId: String?
    public var selectedTagIds: [String]
    public var docmarkType: YabaCoreDocmarkType?
    public var pickedDocumentData: Data?
    public var sourceFileName: String?
    public var previewImageData: Data?
    public var metadataTitle: String?
    public var metadataDescription: String?
    public var metadataAuthor: String?
    public var metadataDate: String?
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
        summary: String = "",
        selectedFolderId: String? = nil,
        selectedTagIds: [String] = [],
        docmarkType: YabaCoreDocmarkType? = .pdf,
        pickedDocumentData: Data? = nil,
        sourceFileName: String? = nil,
        previewImageData: Data? = nil,
        metadataTitle: String? = nil,
        metadataDescription: String? = nil,
        metadataAuthor: String? = nil,
        metadataDate: String? = nil,
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
        self.summary = summary
        self.selectedFolderId = selectedFolderId
        self.selectedTagIds = selectedTagIds
        self.docmarkType = docmarkType
        self.pickedDocumentData = pickedDocumentData
        self.sourceFileName = sourceFileName
        self.previewImageData = previewImageData
        self.metadataTitle = metadataTitle
        self.metadataDescription = metadataDescription
        self.metadataAuthor = metadataAuthor
        self.metadataDate = metadataDate
        self.bookmarkAppearance = bookmarkAppearance
        self.cardImageSizing = cardImageSizing
        self.isPrivate = isPrivate
        self.isPinned = isPinned
        self.isSaving = isSaving
        self.lastError = lastError
    }
}
