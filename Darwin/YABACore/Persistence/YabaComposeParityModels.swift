//
//  YabaComposeParityModels.swift
//  YABACore
//
//  Compose-aligned SwiftData layer (schema v2). Order matters for type resolution.
//

import Foundation
import SwiftData

// MARK: - Folder

@Model
final class FolderModel {
    var folderId: String = UUID().uuidString
    var label: String = ""
    var folderDescription: String?
    var icon: String = "folder-01"
    var colorRaw: Int = 0
    var createdAt: Date = Date.now
    var editedAt: Date = Date.now
    var isHidden: Bool = false

    var parent: FolderModel?

    @Relationship(deleteRule: .cascade, inverse: \FolderModel.parent)
    var children: [FolderModel] = []

    @Relationship(deleteRule: .cascade, inverse: \BookmarkModel.folder)
    var bookmarks: [BookmarkModel] = []

    init(
        folderId: String = UUID().uuidString,
        label: String = "",
        folderDescription: String? = nil,
        icon: String = "folder-01",
        colorRaw: Int = 0,
        createdAt: Date = .now,
        editedAt: Date = .now,
        isHidden: Bool = false,
        parent: FolderModel? = nil,
        children: [FolderModel] = [],
        bookmarks: [BookmarkModel] = []
    ) {
        self.folderId = folderId
        self.label = label
        self.folderDescription = folderDescription
        self.icon = icon
        self.colorRaw = colorRaw
        self.createdAt = createdAt
        self.editedAt = editedAt
        self.isHidden = isHidden
        self.parent = parent
        self.children = children
        self.bookmarks = bookmarks
    }
}

// MARK: - Tag

@Model
final class TagModel {
    var tagId: String = UUID().uuidString
    var label: String = ""
    var icon: String = "tag-01"
    var colorRaw: Int = 0
    var createdAt: Date = Date.now
    var editedAt: Date = Date.now
    var isHidden: Bool = false

    /// Inverse side of `BookmarkModel.tags` (macro only on bookmark side).
    var bookmarks: [BookmarkModel] = []

    init(
        tagId: String = UUID().uuidString,
        label: String = "",
        icon: String = "tag-01",
        colorRaw: Int = 0,
        createdAt: Date = .now,
        editedAt: Date = .now,
        isHidden: Bool = false,
        bookmarks: [BookmarkModel] = []
    ) {
        self.tagId = tagId
        self.label = label
        self.icon = icon
        self.colorRaw = colorRaw
        self.createdAt = createdAt
        self.editedAt = editedAt
        self.isHidden = isHidden
        self.bookmarks = bookmarks
    }
}

// MARK: - Bookmark (base)

@Model
final class BookmarkModel {
    var bookmarkId: String = UUID().uuidString
    var kindRaw: Int = YabaCoreBookmarkKind.link.rawValue
    var label: String = ""
    var bookmarkDescription: String?
    var createdAt: Date = Date.now
    var editedAt: Date = Date.now
    var viewCount: Int = 0
    var isPrivate: Bool = false
    var isPinned: Bool = false

    var folder: FolderModel?

    @Relationship(deleteRule: .nullify, inverse: \TagModel.bookmarks)
    var tags: [TagModel] = []

    @Relationship(deleteRule: .cascade, inverse: \BookmarkImagePayloadModel.bookmark)
    var imagePayload: BookmarkImagePayloadModel?

    @Relationship(deleteRule: .cascade, inverse: \BookmarkIconPayloadModel.bookmark)
    var iconPayload: BookmarkIconPayloadModel?

    @Relationship(deleteRule: .cascade, inverse: \LinkBookmarkModel.bookmark)
    var linkDetail: LinkBookmarkModel?

    @Relationship(deleteRule: .cascade, inverse: \NoteBookmarkModel.bookmark)
    var noteDetail: NoteBookmarkModel?

    @Relationship(deleteRule: .cascade, inverse: \ImageBookmarkModel.bookmark)
    var imageDetail: ImageBookmarkModel?

    @Relationship(deleteRule: .cascade, inverse: \DocBookmarkModel.bookmark)
    var docDetail: DocBookmarkModel?

    @Relationship(deleteRule: .cascade, inverse: \CanvasBookmarkModel.bookmark)
    var canvasDetail: CanvasBookmarkModel?

    @Relationship(deleteRule: .cascade, inverse: \ReadableVersionModel.bookmark)
    var readableVersions: [ReadableVersionModel] = []

    @Relationship(deleteRule: .cascade, inverse: \AnnotationModel.bookmark)
    var annotations: [AnnotationModel] = []

    init(
        bookmarkId: String = UUID().uuidString,
        kindRaw: Int = YabaCoreBookmarkKind.link.rawValue,
        label: String = "",
        bookmarkDescription: String? = nil,
        createdAt: Date = .now,
        editedAt: Date = .now,
        viewCount: Int = 0,
        isPrivate: Bool = false,
        isPinned: Bool = false,
        folder: FolderModel? = nil,
        tags: [TagModel] = []
    ) {
        self.bookmarkId = bookmarkId
        self.kindRaw = kindRaw
        self.label = label
        self.bookmarkDescription = bookmarkDescription
        self.createdAt = createdAt
        self.editedAt = editedAt
        self.viewCount = viewCount
        self.isPrivate = isPrivate
        self.isPinned = isPinned
        self.folder = folder
        self.tags = tags
    }
}

// MARK: - Payloads (lazy Data)

@Model
final class BookmarkImagePayloadModel {
    var bookmarkImagePayloadId: String = UUID().uuidString

    @Attribute(.externalStorage)
    var bytes: Data?

    var bookmark: BookmarkModel?

    init(
        bookmarkImagePayloadId: String = UUID().uuidString,
        bytes: Data? = nil,
        bookmark: BookmarkModel? = nil
    ) {
        self.bookmarkImagePayloadId = bookmarkImagePayloadId
        self.bytes = bytes
        self.bookmark = bookmark
    }
}

@Model
final class BookmarkIconPayloadModel {
    var bookmarkIconPayloadId: String = UUID().uuidString

    @Attribute(.externalStorage)
    var bytes: Data?

    var bookmark: BookmarkModel?

    init(
        bookmarkIconPayloadId: String = UUID().uuidString,
        bytes: Data? = nil,
        bookmark: BookmarkModel? = nil
    ) {
        self.bookmarkIconPayloadId = bookmarkIconPayloadId
        self.bytes = bytes
        self.bookmark = bookmark
    }
}

// MARK: - Link

@Model
final class LinkBookmarkModel {
    var url: String = ""
    var domain: String = ""
    var videoUrl: String?
    var audioUrl: String?
    var metadataTitle: String?
    var metadataDescription: String?
    var metadataAuthor: String?
    var metadataDate: String?

    var bookmark: BookmarkModel?

    init(
        url: String = "",
        domain: String = "",
        videoUrl: String? = nil,
        audioUrl: String? = nil,
        metadataTitle: String? = nil,
        metadataDescription: String? = nil,
        metadataAuthor: String? = nil,
        metadataDate: String? = nil,
        bookmark: BookmarkModel? = nil
    ) {
        self.url = url
        self.domain = domain
        self.videoUrl = videoUrl
        self.audioUrl = audioUrl
        self.metadataTitle = metadataTitle
        self.metadataDescription = metadataDescription
        self.metadataAuthor = metadataAuthor
        self.metadataDate = metadataDate
        self.bookmark = bookmark
    }
}

// MARK: - Image subtype

@Model
final class ImageBookmarkModel {
    var summary: String?

    var bookmark: BookmarkModel?

    init(summary: String? = nil, bookmark: BookmarkModel? = nil) {
        self.summary = summary
        self.bookmark = bookmark
    }
}

// MARK: - Doc subtype

@Model
final class DocBookmarkModel {
    var summary: String?
    var docmarkTypeRaw: String = YabaCoreDocmarkType.pdf.rawValue
    var metadataTitle: String?
    var metadataDescription: String?
    var metadataAuthor: String?
    var metadataDate: String?

    @Relationship(deleteRule: .cascade, inverse: \DocBookmarkPayloadModel.docBookmark)
    var payload: DocBookmarkPayloadModel?

    var bookmark: BookmarkModel?

    init(
        summary: String? = nil,
        docmarkTypeRaw: String = YabaCoreDocmarkType.pdf.rawValue,
        metadataTitle: String? = nil,
        metadataDescription: String? = nil,
        metadataAuthor: String? = nil,
        metadataDate: String? = nil,
        payload: DocBookmarkPayloadModel? = nil,
        bookmark: BookmarkModel? = nil
    ) {
        self.summary = summary
        self.docmarkTypeRaw = docmarkTypeRaw
        self.metadataTitle = metadataTitle
        self.metadataDescription = metadataDescription
        self.metadataAuthor = metadataAuthor
        self.metadataDate = metadataDate
        self.payload = payload
        self.bookmark = bookmark
    }
}

@Model
final class DocBookmarkPayloadModel {
    var docBookmarkPayloadId: String = UUID().uuidString

    @Attribute(.externalStorage)
    var bytes: Data?

    var docBookmark: DocBookmarkModel?

    init(
        docBookmarkPayloadId: String = UUID().uuidString,
        bytes: Data? = nil,
        docBookmark: DocBookmarkModel? = nil
    ) {
        self.docBookmarkPayloadId = docBookmarkPayloadId
        self.bytes = bytes
        self.docBookmark = docBookmark
    }
}

// MARK: - Note subtype

@Model
final class NoteBookmarkModel {
    var readableVersionId: String = ""

    @Relationship(deleteRule: .cascade, inverse: \NoteBookmarkPayloadModel.noteBookmark)
    var payload: NoteBookmarkPayloadModel?

    var bookmark: BookmarkModel?

    init(
        readableVersionId: String = "",
        payload: NoteBookmarkPayloadModel? = nil,
        bookmark: BookmarkModel? = nil
    ) {
        self.readableVersionId = readableVersionId
        self.payload = payload
        self.bookmark = bookmark
    }
}

@Model
final class NoteBookmarkPayloadModel {
    var noteBookmarkPayloadId: String = UUID().uuidString

    @Attribute(.externalStorage)
    var documentBody: Data?

    var noteBookmark: NoteBookmarkModel?

    init(
        noteBookmarkPayloadId: String = UUID().uuidString,
        documentBody: Data? = nil,
        noteBookmark: NoteBookmarkModel? = nil
    ) {
        self.noteBookmarkPayloadId = noteBookmarkPayloadId
        self.documentBody = documentBody
        self.noteBookmark = noteBookmark
    }
}

// MARK: - Canvas subtype

@Model
final class CanvasBookmarkModel {
    @Relationship(deleteRule: .cascade, inverse: \CanvasBookmarkPayloadModel.canvasBookmark)
    var payload: CanvasBookmarkPayloadModel?

    var bookmark: BookmarkModel?

    init(payload: CanvasBookmarkPayloadModel? = nil, bookmark: BookmarkModel? = nil) {
        self.payload = payload
        self.bookmark = bookmark
    }
}

@Model
final class CanvasBookmarkPayloadModel {
    var canvasBookmarkPayloadId: String = UUID().uuidString

    @Attribute(.externalStorage)
    var sceneData: Data?

    var canvasBookmark: CanvasBookmarkModel?

    init(
        canvasBookmarkPayloadId: String = UUID().uuidString,
        sceneData: Data? = nil,
        canvasBookmark: CanvasBookmarkModel? = nil
    ) {
        self.canvasBookmarkPayloadId = canvasBookmarkPayloadId
        self.sceneData = sceneData
        self.canvasBookmark = canvasBookmark
    }
}

// MARK: - Readable version

@Model
final class ReadableVersionModel {
    var readableVersionId: String = UUID().uuidString
    var createdAt: Date = Date.now
    var relativePathHint: String?

    @Relationship(deleteRule: .cascade, inverse: \ReadableVersionPayloadModel.readableVersion)
    var payload: ReadableVersionPayloadModel?

    var bookmark: BookmarkModel?

    @Relationship(deleteRule: .cascade, inverse: \AnnotationModel.readableVersion)
    var annotations: [AnnotationModel] = []

    @Relationship(deleteRule: .cascade, inverse: \ReadableInlineAssetModel.readableVersion)
    var inlineAssets: [ReadableInlineAssetModel] = []

    init(
        readableVersionId: String = UUID().uuidString,
        createdAt: Date = .now,
        relativePathHint: String? = nil,
        payload: ReadableVersionPayloadModel? = nil,
        bookmark: BookmarkModel? = nil
    ) {
        self.readableVersionId = readableVersionId
        self.createdAt = createdAt
        self.relativePathHint = relativePathHint
        self.payload = payload
        self.bookmark = bookmark
    }
}

/// Inline images for a readable version (reader JSON references `../assets/<assetId>.<ext>`); bytes stored for iCloud sync.
@Model
final class ReadableInlineAssetModel {
    var assetId: String = UUID().uuidString
    var pathExtension: String = "jpg"

    @Attribute(.externalStorage)
    var bytes: Data?

    var readableVersion: ReadableVersionModel?

    init(
        assetId: String = UUID().uuidString,
        pathExtension: String = "jpg",
        bytes: Data? = nil,
        readableVersion: ReadableVersionModel? = nil
    ) {
        self.assetId = assetId
        self.pathExtension = pathExtension
        self.bytes = bytes
        self.readableVersion = readableVersion
    }
}

@Model
final class ReadableVersionPayloadModel {
    var readableVersionPayloadId: String = UUID().uuidString

    @Attribute(.externalStorage)
    var documentJson: Data?

    var readableVersion: ReadableVersionModel?

    init(
        readableVersionPayloadId: String = UUID().uuidString,
        documentJson: Data? = nil,
        readableVersion: ReadableVersionModel? = nil
    ) {
        self.readableVersionPayloadId = readableVersionPayloadId
        self.documentJson = documentJson
        self.readableVersion = readableVersion
    }
}

// MARK: - Annotation

@Model
final class AnnotationModel {
    var annotationId: String = UUID().uuidString
    var typeRaw: String = YabaCoreAnnotationType.readable.rawValue
    var colorRoleRaw: Int = 0
    var note: String?
    var quoteText: String?
    var extrasJson: String?
    var createdAt: Date = Date.now
    var editedAt: Date = Date.now

    var bookmark: BookmarkModel?

    var readableVersion: ReadableVersionModel?

    init(
        annotationId: String = UUID().uuidString,
        typeRaw: String = YabaCoreAnnotationType.readable.rawValue,
        colorRoleRaw: Int = 0,
        note: String? = nil,
        quoteText: String? = nil,
        extrasJson: String? = nil,
        createdAt: Date = .now,
        editedAt: Date = .now,
        bookmark: BookmarkModel? = nil,
        readableVersion: ReadableVersionModel? = nil
    ) {
        self.annotationId = annotationId
        self.typeRaw = typeRaw
        self.colorRoleRaw = colorRoleRaw
        self.note = note
        self.quoteText = quoteText
        self.extrasJson = extrasJson
        self.createdAt = createdAt
        self.editedAt = editedAt
        self.bookmark = bookmark
        self.readableVersion = readableVersion
    }
}

