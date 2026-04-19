//
//  BookmarkFlowHydration.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import Foundation
import SwiftData

enum BookmarkFlowHydration {
    @MainActor
    static func fetchBookmark(bookmarkId: String, modelContext: ModelContext) -> BookmarkModel? {
        let bid = bookmarkId
        var descriptor = FetchDescriptor<BookmarkModel>(
            predicate: #Predicate { $0.bookmarkId == bid }
        )
        descriptor.fetchLimit = 1
        return try? modelContext.fetch(descriptor).first
    }

    @MainActor
    static func linkmarkUIState(from bookmark: BookmarkModel) -> LinkmarkCreationUIState {
        var state = LinkmarkCreationUIState()
        state.editingBookmarkId = bookmark.bookmarkId
        state.label = bookmark.label
        state.bookmarkDescription = bookmark.bookmarkDescription ?? ""
        state.selectedFolderId = bookmark.folder?.folderId
        state.selectedTagIds = bookmark.tags.map(\.tagId)
        state.isPinned = bookmark.isPinned

        if let link = bookmark.linkDetail {
            state.url = link.url
            state.metadataTitle = link.metadataTitle
            state.metadataDescription = link.metadataDescription
            state.metadataAuthor = link.metadataAuthor
            state.metadataDate = link.metadataDate
            state.videoUrl = link.videoUrl
            state.audioUrl = link.audioUrl
        }

        state.previewImageData = bookmark.imagePayload?.bytes
        state.previewIconData = bookmark.iconPayload?.bytes
        state.uncategorizedFolderCreationRequired = false
        return state
    }

    @MainActor
    static func notemarkUIState(from bookmark: BookmarkModel) -> NotemarkCreationUIState {
        var state = NotemarkCreationUIState()
        state.editingBookmarkId = bookmark.bookmarkId
        state.label = bookmark.label
        state.bookmarkDescription = bookmark.bookmarkDescription ?? ""
        state.selectedFolderId = bookmark.folder?.folderId
        state.selectedTagIds = bookmark.tags.map(\.tagId)
        state.isPinned = bookmark.isPinned

        if let body = bookmark.noteDetail?.payload?.documentBody,
           let json = String(data: body, encoding: .utf8)
        {
            state.documentJson = json
        }
        state.uncategorizedFolderCreationRequired = false
        return state
    }

    @MainActor
    static func imagemarkUIState(from bookmark: BookmarkModel) -> ImagemarkCreationUIState {
        var state = ImagemarkCreationUIState()
        state.editingBookmarkId = bookmark.bookmarkId
        state.label = bookmark.label
        state.bookmarkDescription = bookmark.bookmarkDescription ?? ""
        state.summary = bookmark.imageDetail?.summary ?? ""
        state.selectedFolderId = bookmark.folder?.folderId
        state.selectedTagIds = bookmark.tags.map(\.tagId)
        state.isPinned = bookmark.isPinned
        state.imageData = bookmark.imagePayload?.bytes
        state.uncategorizedFolderCreationRequired = false
        return state
    }

    @MainActor
    static func docmarkUIState(from bookmark: BookmarkModel) -> DocmarkCreationUIState {
        var state = DocmarkCreationUIState()
        state.editingBookmarkId = bookmark.bookmarkId
        state.label = bookmark.label
        state.bookmarkDescription = bookmark.bookmarkDescription ?? ""
        state.summary = bookmark.docDetail?.summary ?? ""
        state.selectedFolderId = bookmark.folder?.folderId
        state.selectedTagIds = bookmark.tags.map(\.tagId)
        state.isPinned = bookmark.isPinned
        if let raw = bookmark.docDetail?.docmarkTypeRaw {
            state.docmarkType = DocmarkType(rawValue: raw)
        }
        if let doc = bookmark.docDetail {
            state.metadataTitle = doc.metadataTitle
            state.metadataDescription = doc.metadataDescription
            state.metadataAuthor = doc.metadataAuthor
            state.metadataDate = doc.metadataDate
        }
        state.previewImageData = bookmark.imagePayload?.bytes
        state.uncategorizedFolderCreationRequired = false
        return state
    }

    @MainActor
    static func canvmarkUIState(from bookmark: BookmarkModel) -> CanvmarkCreationUIState {
        var state = CanvmarkCreationUIState()
        state.editingBookmarkId = bookmark.bookmarkId
        state.label = bookmark.label
        state.bookmarkDescription = bookmark.bookmarkDescription ?? ""
        state.selectedFolderId = bookmark.folder?.folderId
        state.selectedTagIds = bookmark.tags.map(\.tagId)
        state.isPinned = bookmark.isPinned
        state.sceneData = bookmark.canvasDetail?.payload?.sceneData
        state.uncategorizedFolderCreationRequired = false
        return state
    }
}
