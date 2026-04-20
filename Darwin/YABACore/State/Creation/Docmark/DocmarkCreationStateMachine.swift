//
//  DocmarkCreationStateMachine.swift
//  YABACore
//

import Foundation
import SwiftUI

@MainActor
public final class DocmarkCreationStateMachine: YabaBaseObservableState<DocmarkCreationUIState>, YabaScreenStateMachine {
    public override init(initialState: DocmarkCreationUIState = DocmarkCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: DocmarkCreationEvent) async {
        switch event {
        case let .onInit(id, folderId, tagIds, uncategorizedFolderCreationRequired):
            apply {
                $0.editingBookmarkId = id
                $0.selectedFolderId = folderId
                $0.selectedTagIds = tagIds ?? []
                if id == nil {
                    $0.uncategorizedFolderCreationRequired = uncategorizedFolderCreationRequired
                } else {
                    $0.uncategorizedFolderCreationRequired = false
                }
            }
        case .onPickDocument:
            break
        case .onClearDocument:
            apply {
                $0.pickedDocumentData = nil
                $0.sourceFileName = nil
            }
        case let .onDocumentFromShare(data, name, type):
            apply {
                $0.pickedDocumentData = data
                $0.sourceFileName = name
                $0.docmarkType = type
            }
        case .onCyclePreviewAppearance:
            apply {
                switch $0.bookmarkAppearance {
                case .list: $0.bookmarkAppearance = .card
                case .card: $0.bookmarkAppearance = .grid
                case .grid: $0.bookmarkAppearance = .list
                }
            }
        case let .onDocumentMetadataExtracted(title, desc, author, date):
            apply {
                $0.metadataTitle = title
                $0.metadataDescription = desc
                $0.metadataAuthor = author
                $0.metadataDate = date
            }
        case let .onSetGeneratedPreview(data, _):
            apply { $0.previewImageData = data }
        case let .onChangeLabel(s):
            apply { $0.label = s }
        case let .onChangeDescription(s):
            apply { $0.bookmarkDescription = s }
        case .onApplyFromMetadata:
            withAnimation {
                apply {
                    if let t = $0.metadataTitle, !t.isEmpty { $0.label = t }
                    if let d = $0.metadataDescription, !d.isEmpty { $0.bookmarkDescription = d }
                }
            }
        case let .onSelectFolderId(id):
            apply {
                $0.selectedFolderId = id
                $0.uncategorizedFolderCreationRequired = false
            }
        case let .onSelectTagIds(ids):
            apply { $0.selectedTagIds = ids }
        case .onWebInitialContentLoad(_):
            break
        case .onSave:
            await persist()
        case .onTogglePinned:
            apply { $0.isPinned.toggle() }
        case let .create(bookmarkId, folderId, label, bookmarkDescription, isPinned, tagIds):
            AllBookmarksManager.queueCreateBookmark(
                bookmarkId: bookmarkId,
                folderId: folderId,
                kind: .file,
                label: label,
                bookmarkDescription: bookmarkDescription,
                isPinned: isPinned,
                tagIds: tagIds
            )
            DocmarkManager.queueEnsureInitialDocDetail(bookmarkId: bookmarkId)
        }
    }

    private func persist() async {
        let folderId = state.selectedFolderId
        let label = state.label.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let folderId, !folderId.isEmpty else {
            apply { $0.lastError = "Folder required" }
            return
        }
        if state.uncategorizedFolderCreationRequired {
            do {
                try await CoreOperationQueue.shared.queueAndAwait(name: "EnsureUncategorizedFolderVisible") { context in
                    try FolderManager.ensureUncategorizedFolderVisibleInContext(context)
                }
                apply { $0.uncategorizedFolderCreationRequired = false }
            } catch {
                apply {
                    $0.lastError = String(describing: error)
                    $0.isSaving = false
                }
                return
            }
        }
        guard !label.isEmpty else {
            apply { $0.lastError = "Label required" }
            return
        }
        apply { $0.lastError = nil; $0.isSaving = true }
        let bid = state.editingBookmarkId ?? UUID().uuidString
        if state.editingBookmarkId != nil {
            AllBookmarksManager.queueUpdateBookmarkMetadata(
                bookmarkId: bid,
                folderId: folderId,
                kind: .file,
                label: label,
                bookmarkDescription: state.bookmarkDescription.nilIfEmpty,
                isPinned: state.isPinned,
                tagIds: state.selectedTagIds
            )
        } else {
            AllBookmarksManager.queueCreateBookmark(
                bookmarkId: bid,
                folderId: folderId,
                kind: .file,
                label: label,
                bookmarkDescription: state.bookmarkDescription.nilIfEmpty,
                isPinned: state.isPinned,
                tagIds: state.selectedTagIds
            )
        }
        DocmarkManager.queueCreateOrUpdateDocDetails(
            bookmarkId: bid,
            summary: nil,
            docmarkType: state.docmarkType,
            metadataTitle: state.metadataTitle,
            metadataDescription: state.metadataDescription,
            metadataAuthor: state.metadataAuthor,
            metadataDate: state.metadataDate
        )
        apply { $0.isSaving = false }
    }
}
