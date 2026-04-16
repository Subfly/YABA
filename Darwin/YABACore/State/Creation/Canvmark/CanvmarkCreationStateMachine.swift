//
//  CanvmarkCreationStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class CanvmarkCreationStateMachine: YabaBaseObservableState<CanvmarkCreationUIState>, YabaScreenStateMachine {
    public override init(initialState: CanvmarkCreationUIState = CanvmarkCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: CanvmarkCreationEvent) async {
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
            apply {
                $0.selectedFolderId = id
                $0.uncategorizedFolderCreationRequired = false
            }
        case let .onSelectTagIds(ids):
            apply { $0.selectedTagIds = ids }
        case .onSave:
            await persist()
        case .onTogglePrivate:
            apply { $0.isPrivate.toggle() }
        case .onTogglePinned:
            apply { $0.isPinned.toggle() }
        case let .create(bookmarkId, folderId, label, bookmarkDescription, isPrivate, isPinned, tagIds):
            AllBookmarksManager.queueCreateBookmark(
                bookmarkId: bookmarkId,
                folderId: folderId,
                kind: .canvas,
                label: label,
                bookmarkDescription: bookmarkDescription,
                isPrivate: isPrivate,
                isPinned: isPinned,
                tagIds: tagIds
            )
            CanvmarkManager.queueCreateOrUpdateCanvasDetails(bookmarkId: bookmarkId)
        case let .bootstrapCanvas(bookmarkId):
            CanvmarkManager.queueCreateOrUpdateCanvasDetails(bookmarkId: bookmarkId)
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
                kind: .canvas,
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
                kind: .canvas,
                label: label,
                bookmarkDescription: state.bookmarkDescription.nilIfEmpty,
                isPrivate: state.isPrivate,
                isPinned: state.isPinned,
                tagIds: state.selectedTagIds
            )
        }
        if let data = state.sceneData {
            CanvmarkManager.queueSaveCanvasSceneData(bookmarkId: bid, sceneData: data)
        }
        CanvmarkManager.queueCreateOrUpdateCanvasDetails(bookmarkId: bid)
        apply { $0.isSaving = false }
    }
}
