//
//  LinkmarkCreationUIState.swift
//  YABACore
//

import Foundation

public struct LinkmarkCreationUIState: Sendable {
    public var editingBookmarkId: String?
    public var url: String
    public var label: String
    public var bookmarkDescription: String
    public var selectedFolderId: String?
    public var selectedTagIds: [String]
    public var metadataTitle: String?
    public var metadataDescription: String?
    public var metadataAuthor: String?
    public var metadataDate: String?
    public var videoUrl: String?
    public var audioUrl: String?
    public var isPrivate: Bool
    public var isPinned: Bool
    public var bookmarkAppearance: YabaCoreBookmarkAppearance
    public var cardImageSizing: YabaCoreCardImageSizing
    public var isSaving: Bool
    public var lastError: String?
    public var converterError: String?

    /// True while fetching + converting URL content in Core.
    public var isFetchingLinkContent: Bool
    public var lastFetchedUrl: String?
    public var cleanedUrl: String?
    public var previewImageData: Data?
    public var previewIconData: Data?
    /// Latest processed readable (for save when bookmark exists or on first save).
    public var pendingReadableUnfurl: YabaDarwinReadableUnfurl?

    public init(
        editingBookmarkId: String? = nil,
        url: String = "",
        label: String = "",
        bookmarkDescription: String = "",
        selectedFolderId: String? = nil,
        selectedTagIds: [String] = [],
        metadataTitle: String? = nil,
        metadataDescription: String? = nil,
        metadataAuthor: String? = nil,
        metadataDate: String? = nil,
        videoUrl: String? = nil,
        audioUrl: String? = nil,
        isPrivate: Bool = false,
        isPinned: Bool = false,
        bookmarkAppearance: YabaCoreBookmarkAppearance = .list,
        cardImageSizing: YabaCoreCardImageSizing = .small,
        isSaving: Bool = false,
        lastError: String? = nil,
        converterError: String? = nil,
        isFetchingLinkContent: Bool = false,
        lastFetchedUrl: String? = nil,
        cleanedUrl: String? = nil,
        previewImageData: Data? = nil,
        previewIconData: Data? = nil,
        pendingReadableUnfurl: YabaDarwinReadableUnfurl? = nil
    ) {
        self.editingBookmarkId = editingBookmarkId
        self.url = url
        self.label = label
        self.bookmarkDescription = bookmarkDescription
        self.selectedFolderId = selectedFolderId
        self.selectedTagIds = selectedTagIds
        self.metadataTitle = metadataTitle
        self.metadataDescription = metadataDescription
        self.metadataAuthor = metadataAuthor
        self.metadataDate = metadataDate
        self.videoUrl = videoUrl
        self.audioUrl = audioUrl
        self.isPrivate = isPrivate
        self.isPinned = isPinned
        self.bookmarkAppearance = bookmarkAppearance
        self.cardImageSizing = cardImageSizing
        self.isSaving = isSaving
        self.lastError = lastError
        self.converterError = converterError
        self.isFetchingLinkContent = isFetchingLinkContent
        self.lastFetchedUrl = lastFetchedUrl
        self.cleanedUrl = cleanedUrl
        self.previewImageData = previewImageData
        self.previewIconData = previewIconData
        self.pendingReadableUnfurl = pendingReadableUnfurl
    }
}
