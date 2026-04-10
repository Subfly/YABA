//
//  NotemarkCreationStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class NotemarkCreationStateMachine: YabaBaseObservableState<NotemarkCreationUIState>, YabaScreenStateMachine {
    public override init(initialState: NotemarkCreationUIState = NotemarkCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: NotemarkCreationEvent) async {
        switch event {
        case let .onInit(id, folderId, tagIds):
            apply {
                $0.editingBookmarkId = id
                $0.selectedFolderId = folderId
                $0.selectedTagIds = tagIds ?? []
            }
        case .onCyclePreviewAppearance:
            apply {
                switch $0.bookmarkAppearance {
                case .list: $0.bookmarkAppearance = .card
                case .card: $0.bookmarkAppearance = .grid
                case .grid: $0.bookmarkAppearance = .list
                }
            }
        case let .onChangeLabel(s):
            apply { $0.label = s }
        case let .onChangeDescription(s):
            apply { $0.bookmarkDescription = s }
        case let .onSelectFolderId(id):
            apply { $0.selectedFolderId = id }
        case let .onSelectTagIds(ids):
            apply { $0.selectedTagIds = ids }
        case .onSave:
            persist()
        case .onTogglePrivate:
            apply { $0.isPrivate.toggle() }
        case .onTogglePinned:
            apply { $0.isPinned.toggle() }
        case let .createBookmark(bookmarkId, folderId, label, bookmarkDescription, isPrivate, isPinned, tagIds):
            AllBookmarksManager.queueCreateBookmark(
                bookmarkId: bookmarkId,
                folderId: folderId,
                kind: .note,
                label: label,
                bookmarkDescription: bookmarkDescription,
                isPrivate: isPrivate,
                isPinned: isPinned,
                tagIds: tagIds
            )
            NotemarkManager.queueCreateOrUpdateNoteDetails(bookmarkId: bookmarkId)
        case let .bootstrapNoteSubtype(bookmarkId):
            NotemarkManager.queueCreateOrUpdateNoteDetails(bookmarkId: bookmarkId)
        }
    }

    private func persist() {
        let folderId = state.selectedFolderId
        let label = state.label.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let folderId, !folderId.isEmpty else {
            apply { $0.lastError = "Folder required" }
            return
        }
        guard !label.isEmpty else {
            apply { $0.lastError = "Label required" }
            return
        }
        apply { $0.lastError = nil; $0.isSaving = true }
        let bid = state.editingBookmarkId ?? UUID().uuidString
        let versionId = UUID().uuidString
        if state.editingBookmarkId != nil {
            AllBookmarksManager.queueUpdateBookmarkMetadata(
                bookmarkId: bid,
                folderId: folderId,
                kind: .note,
                label: label,
                bookmarkDescription: state.bookmarkDescription.nilIfEmpty,
                isPrivate: state.isPrivate,
                isPinned: state.isPinned,
                tagIds: state.selectedTagIds
            )
        } else {
            AllBookmarksManager.queueCreateBookmark(
                bookmarkId: bid,
                folderId: folderId,
                kind: .note,
                label: label,
                bookmarkDescription: state.bookmarkDescription.nilIfEmpty,
                isPrivate: state.isPrivate,
                isPinned: state.isPinned,
                tagIds: state.selectedTagIds
            )
        }
        let data = Data(state.documentJson.utf8)
        NotemarkManager.queueSaveNoteDocumentData(bookmarkId: bid, documentBody: data)
        ReadableContentManager.queueSyncNotemarkReadableMirror(bookmarkId: bid, versionId: versionId, documentJson: state.documentJson)
        NotemarkManager.queueCreateOrUpdateNoteDetails(bookmarkId: bid, readableVersionId: versionId)
        apply { $0.isSaving = false }
    }
}
