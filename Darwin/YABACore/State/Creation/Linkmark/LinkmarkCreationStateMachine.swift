//
//  LinkmarkCreationStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class LinkmarkCreationStateMachine: YabaBaseObservableState<LinkmarkCreationUIState>, YabaScreenStateMachine {
    public override init(initialState: LinkmarkCreationUIState = LinkmarkCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: LinkmarkCreationEvent) async {
        switch event {
        case let .onInit(linkmarkId, initialUrl, initialFolderId, initialTagIds):
            apply {
                $0.editingBookmarkId = linkmarkId
                if let u = initialUrl { $0.url = u }
                $0.selectedFolderId = initialFolderId
                $0.selectedTagIds = initialTagIds ?? []
            }
        case .onCyclePreviewAppearance:
            apply {
                switch $0.bookmarkAppearance {
                case .list: $0.bookmarkAppearance = .card
                case .card: $0.bookmarkAppearance = .grid
                case .grid: $0.bookmarkAppearance = .list
                }
            }
        case let .onChangeUrl(u):
            apply { $0.url = u }
        case let .onChangeLabel(l):
            apply { $0.label = l }
        case let .onChangeDescription(d):
            apply { $0.bookmarkDescription = d }
        case let .onSelectFolderId(id):
            apply { $0.selectedFolderId = id }
        case let .onSelectTagIds(ids):
            apply { $0.selectedTagIds = ids }
        case .onClearLabel:
            apply { $0.label = "" }
        case .onClearDescription:
            apply { $0.bookmarkDescription = "" }
        case .onApplyFromMetadata:
            apply {
                if let t = $0.metadataTitle, !t.isEmpty { $0.label = t }
                if let d = $0.metadataDescription, !d.isEmpty { $0.bookmarkDescription = d }
            }
        case .onRefetch:
            break
        case let .onConverterSucceeded(_, metaJson):
            apply { __ in _ = metaJson }
        case let .onConverterFailed(err):
            apply { $0.converterError = err }
        case .onSave:
            persistFromState()
        case .onTogglePrivate:
            apply { $0.isPrivate.toggle() }
        case .onTogglePinned:
            apply { $0.isPinned.toggle() }
        case let .create(
            bookmarkId,
            folderId,
            label,
            bookmarkDescription,
            isPrivate,
            isPinned,
            tagIds,
            url,
            domain,
            videoUrl,
            audioUrl,
            metadataTitle,
            metadataDescription,
            metadataAuthor,
            metadataDate
        ):
            apply { $0.isSaving = true }
            AllBookmarksManager.queueCreateBookmark(
                bookmarkId: bookmarkId,
                folderId: folderId,
                kind: .link,
                label: label,
                bookmarkDescription: bookmarkDescription,
                isPrivate: isPrivate,
                isPinned: isPinned,
                tagIds: tagIds
            )
            LinkmarkManager.queueCreateOrUpdateLinkDetails(
                bookmarkId: bookmarkId,
                url: url,
                domain: domain,
                videoUrl: videoUrl,
                audioUrl: audioUrl,
                metadataTitle: metadataTitle,
                metadataDescription: metadataDescription,
                metadataAuthor: metadataAuthor,
                metadataDate: metadataDate
            )
            apply { $0.isSaving = false }
        }
    }

    private func persistFromState() {
        let folderId = state.selectedFolderId
        let label = state.label.trimmingCharacters(in: .whitespacesAndNewlines)
        let url = state.url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let folderId, !folderId.isEmpty else {
            apply { $0.lastError = "Folder required" }
            return
        }
        guard !label.isEmpty else {
            apply { $0.lastError = "Label required" }
            return
        }
        guard !url.isEmpty else {
            apply { $0.lastError = "URL required" }
            return
        }
        apply { $0.lastError = nil; $0.isSaving = true }
        let domain = LinkmarkManager.extractDomain(from: url)
        let videoUrl = state.videoUrl
        let audioUrl = state.audioUrl
        let metadataTitle = state.metadataTitle
        let metadataDescription = state.metadataDescription
        let metadataAuthor = state.metadataAuthor
        let metadataDate = state.metadataDate
        if let bid = state.editingBookmarkId {
            AllBookmarksManager.queueUpdateBookmarkMetadata(
                bookmarkId: bid,
                folderId: folderId,
                kind: .link,
                label: label,
                bookmarkDescription: state.bookmarkDescription.nilIfEmpty,
                isPrivate: state.isPrivate,
                isPinned: state.isPinned,
                tagIds: state.selectedTagIds
            )
            LinkmarkManager.queueCreateOrUpdateLinkDetails(
                bookmarkId: bid,
                url: url,
                domain: domain,
                videoUrl: videoUrl,
                audioUrl: audioUrl,
                metadataTitle: metadataTitle,
                metadataDescription: metadataDescription,
                metadataAuthor: metadataAuthor,
                metadataDate: metadataDate
            )
        } else {
            let bookmarkId = UUID().uuidString
            AllBookmarksManager.queueCreateBookmark(
                bookmarkId: bookmarkId,
                folderId: folderId,
                kind: .link,
                label: label,
                bookmarkDescription: state.bookmarkDescription.nilIfEmpty,
                isPrivate: state.isPrivate,
                isPinned: state.isPinned,
                tagIds: state.selectedTagIds
            )
            LinkmarkManager.queueCreateOrUpdateLinkDetails(
                bookmarkId: bookmarkId,
                url: url,
                domain: domain,
                videoUrl: videoUrl,
                audioUrl: audioUrl,
                metadataTitle: metadataTitle,
                metadataDescription: metadataDescription,
                metadataAuthor: metadataAuthor,
                metadataDate: metadataDate
            )
        }
        apply { $0.isSaving = false }
    }
}
