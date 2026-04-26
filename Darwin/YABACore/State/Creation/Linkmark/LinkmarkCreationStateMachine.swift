//
//  LinkmarkCreationStateMachine.swift
//  YABACore
//

import Foundation
import SwiftUI

@MainActor
public final class LinkmarkCreationStateMachine: YabaBaseObservableState<LinkmarkCreationUIState>, YabaScreenStateMachine {
    private var linkFetchTask: Task<Void, Never>?
    private static let urlDebounceNs: UInt64 = 450_000_000

    public override init(initialState: LinkmarkCreationUIState = LinkmarkCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: LinkmarkCreationEvent) async {
        switch event {
        case let .onInit(linkmarkId, initialUrl, initialFolderId, initialTagIds, uncategorizedFolderCreationRequired):
            apply {
                $0.editingBookmarkId = linkmarkId
                if let u = initialUrl { $0.url = u }
                $0.selectedFolderId = initialFolderId
                $0.selectedTagIds = initialTagIds ?? []
                if linkmarkId == nil {
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
        case let .onChangeUrl(u):
            apply { $0.url = u }
            scheduleDebouncedLinkFetch()
        case let .onChangeLabel(l):
            apply { $0.label = l }
        case let .onChangeDescription(d):
            apply { $0.bookmarkDescription = d }
        case let .onSelectFolderId(id):
            apply {
                $0.selectedFolderId = id
                $0.uncategorizedFolderCreationRequired = false
            }
        case let .onSelectTagIds(ids):
            apply { $0.selectedTagIds = ids }
        case .onClearLabel:
            apply { $0.label = "" }
        case .onClearDescription:
            apply { $0.bookmarkDescription = "" }
        case .onApplyFromMetadata:
            withAnimation {
                apply {
                    if let t = $0.metadataTitle, !t.isEmpty { $0.label = t }
                    if let d = $0.metadataDescription, !d.isEmpty { $0.bookmarkDescription = d }
                }
            }
        case .onRefetch:
            await runLinkFetch()
        case .onSave:
            await persistFromState()
        case .onTogglePinned:
            apply { $0.isPinned.toggle() }
        case let .create(
            bookmarkId,
            folderId,
            label,
            bookmarkDescription,
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

    private func scheduleDebouncedLinkFetch() {
        linkFetchTask?.cancel()
        linkFetchTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: Self.urlDebounceNs)
            guard !Task.isCancelled, let self else { return }
            await self.runLinkFetch()
        }
    }

    private func runLinkFetch() async {
        let trimmed = state.url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed.contains(".") else { return }
        if trimmed == state.lastFetchedUrl, state.converterError == nil { return }

        apply { $0.isFetchingLinkContent = true; $0.converterError = nil }
        defer { apply { $0.isFetchingLinkContent = false } }
        do {
            let pack = try await Unfurler.unfurl(trimmed)
            await applyNativeUnfurlResult(pack)
        } catch {
            apply {
                $0.converterError = String(describing: error)
            }
        }
    }

    private func applyNativeUnfurlResult(_ pack: LinkUnfurlResult) async {
        await applyFetchResult(
            metadata: pack.metadata,
            readable: pack.readable,
            previewImage: pack.previewImageData,
            previewIcon: pack.previewIconData
        )
    }

    private func applyFetchResult(
        metadata: LinkMetadataResult,
        readable: ReadableUnfurl,
        previewImage: Data?,
        previewIcon: Data?
    ) async {
        let urlTrim = state.url.trimmingCharacters(in: .whitespacesAndNewlines)
        apply {
            $0.lastFetchedUrl = urlTrim
            $0.cleanedUrl = metadata.cleanedUrl.nilIfEmpty
            $0.metadataTitle = metadata.title
            $0.metadataDescription = metadata.description
            $0.metadataAuthor = metadata.author
            $0.metadataDate = metadata.date
            $0.videoUrl = metadata.video
            $0.audioUrl = metadata.audio
            $0.previewImageData = previewImage
            $0.previewIconData = previewIcon
            $0.pendingReadableUnfurl = readable
            $0.converterError = nil
        }
        ReadableAssetResolver.shared.register(unfurl: readable)
        // Do not persist readable versions on fetch while editing; user saves via Done (upserts latest) or uses detail refresh.
    }

    private func persistFromState() async {
        let folderId = state.selectedFolderId
        let label = state.label.trimmingCharacters(in: .whitespacesAndNewlines)
        let url = state.url.trimmingCharacters(in: .whitespacesAndNewlines)
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
            if let unfurl = state.pendingReadableUnfurl {
                ReadableContentManager.queueUpsertLinkReadableUnfurlFromBookmarkEditor(
                    bookmarkId: bid,
                    unfurl: unfurl
                )
                AllBookmarksManager.queueSetBookmarkPreviewAssets(
                    bookmarkId: bid,
                    imageBytes: state.previewImageData,
                    iconBytes: state.previewIconData
                )
            }
        } else {
            let bookmarkId = UUID().uuidString
            AllBookmarksManager.queueCreateBookmark(
                bookmarkId: bookmarkId,
                folderId: folderId,
                kind: .link,
                label: label,
                bookmarkDescription: state.bookmarkDescription.nilIfEmpty,
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
            if let unfurl = state.pendingReadableUnfurl {
                ReadableContentManager.queueUpsertLinkReadableUnfurlFromBookmarkEditor(
                    bookmarkId: bookmarkId,
                    unfurl: unfurl
                )
                AllBookmarksManager.queueSetBookmarkPreviewAssets(
                    bookmarkId: bookmarkId,
                    imageBytes: state.previewImageData,
                    iconBytes: state.previewIconData
                )
            }
        }
        apply { $0.isSaving = false }
    }
}
