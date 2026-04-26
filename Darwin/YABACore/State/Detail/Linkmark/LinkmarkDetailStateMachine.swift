//
//  LinkmarkDetailStateMachine.swift
//  YABACore
//

import Foundation
import SwiftUI

@MainActor
public final class LinkmarkDetailStateMachine: YabaBaseObservableState<LinkmarkDetailUIState>, YabaScreenStateMachine {
    public override init(initialState: LinkmarkDetailUIState = LinkmarkDetailUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: LinkmarkDetailEvent) async {
        switch event {
        case let .onInit(bookmarkId):
            let reminderDate = await ReminderManager.getPendingReminderDate(bookmarkId: bookmarkId)
            let granted = await ReminderManager.authorizationGranted()
            apply {
                $0.bookmarkId = bookmarkId
                $0.reminderDate = reminderDate
                $0.hasNotificationPermission = granted
            }
        case let .onLinkSourceUrl(url):
            apply { $0.linkSourceUrl = url }
        case let .onSaveReadableContent(data):
            guard let bid = state.bookmarkId else { return }
            let rv = state.selectedReadableVersionId ?? UUID().uuidString
            apply { $0.selectedReadableVersionId = rv }
            ReadableVersionManager.queueInsertReadableVersion(
                bookmarkId: bid,
                readableVersionId: rv,
                markdown: data
            )
        case .onUpdateReadableRequested:
            await refreshReadableFromSource()
        case .onUpdateLinkMetadataRequested:
            await refreshLinkMetadataOnly()
        case let .onReaderWebInitialContentLoad(resultJson):
            apply { $0.readerWebInitialLoadResultJson = resultJson }
        case let .onSelectReadableVersion(versionId):
            apply { $0.selectedReadableVersionId = versionId }
        case let .onDeleteReadableVersion(versionId):
            ReadableVersionManager.queueDeleteReadableVersion(readableVersionId: versionId)
        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])
        case .onToggleReaderTheme:
            apply {
                switch $0.readerTheme {
                case .system: $0.readerTheme = .light
                case .light: $0.readerTheme = .dark
                case .dark: $0.readerTheme = .sepia
                case .sepia: $0.readerTheme = .system
                }
            }
        case .onToggleReaderFontSize:
            apply {
                switch $0.readerFontSize {
                case .small: $0.readerFontSize = .medium
                case .medium: $0.readerFontSize = .large
                case .large: $0.readerFontSize = .small
                }
            }
        case .onToggleReaderLineHeight:
            apply {
                $0.readerLineHeight = $0.readerLineHeight == .normal ? .relaxed : .normal
            }
        case let .onSetReaderTheme(t):
            apply { $0.readerTheme = t }
        case let .onSetReaderFontSize(s):
            apply { $0.readerFontSize = s }
        case let .onSetReaderLineHeight(l):
            apply { $0.readerLineHeight = l }
        case let .onCreateAnnotation(annotationId, readableVersionId, colorRole, note, quoteText):
            guard let bid = state.bookmarkId else { return }
            AnnotationManager.queueInsertAnnotation(
                bookmarkId: bid,
                readableVersionId: readableVersionId,
                type: .readable,
                annotationId: annotationId,
                colorRoleRaw: colorRole.rawValue,
                note: note,
                quoteText: quoteText,
                extrasJson: nil
            )
        case let .onUpdateAnnotation(annotationId, colorRole, note):
            AnnotationManager.queueUpdateAnnotation(
                annotationId: annotationId,
                colorRoleRaw: colorRole.rawValue,
                note: note
            )
        case let .onDeleteAnnotation(annotationId):
            AnnotationManager.queueDeleteAnnotation(annotationId: annotationId)
        case let .onAnnotationReadableCreateCommitted(request, annotationId, html):
            guard let bid = state.bookmarkId else { return }
            let versionId = request.selectionDraft.readableVersionId
            ReadableContentManager.queueSyncNotemarkReadableMirror(
                bookmarkId: bid,
                versionId: versionId,
                html: html
            )
            AnnotationManager.queueInsertAnnotation(
                bookmarkId: bid,
                readableVersionId: versionId,
                type: .readable,
                annotationId: annotationId,
                colorRoleRaw: request.colorRole.rawValue,
                note: request.note?.nilIfEmpty,
                quoteText: request.selectionDraft.quoteText,
                extrasJson: nil
            )
        case let .onAnnotationReadableDeleteCommitted(annotationId, readableVersionId, html):
            guard let bid = state.bookmarkId else { return }
            ReadableContentManager.queueSyncNotemarkReadableMirror(
                bookmarkId: bid,
                versionId: readableVersionId,
                html: html
            )
            AnnotationManager.queueDeleteAnnotation(annotationId: annotationId)
        case let .onScrollToAnnotation(annotationId):
            apply { $0.scrollToAnnotationId = annotationId }
        case .onClearScrollToAnnotation:
            apply { $0.scrollToAnnotationId = nil }
        case let .onTocChanged(tocJson):
            apply { $0.tocJson = tocJson }
        case let .onNavigateToTocItem(id, extrasJson):
            apply {
                $0.pendingTocNavigationId = id
                $0.pendingTocNavigationExtrasJson = extrasJson
            }
        case .onClearTocNavigation:
            apply {
                $0.pendingTocNavigationId = nil
                $0.pendingTocNavigationExtrasJson = nil
            }
        case .onRequestNotificationPermission:
            _ = await ReminderManager.requestAuthorization()
            let granted = await ReminderManager.authorizationGranted()
            apply { $0.hasNotificationPermission = granted }
            if !granted {
                CoreToastManager.shared.showNotificationPermissionDeniedToast()
            }
        case let .onScheduleReminder(fireAt, titleKey, messageKey):
            guard let bid = state.bookmarkId else { return }
            do {
                try await ReminderManager.scheduleReminderResolvingLabel(
                    bookmarkId: bid,
                    bookmarkKindCode: BookmarkKind.link.rawValue,
                    titleKey: titleKey,
                    messageKey: messageKey,
                    fireAt: fireAt
                )
                apply { $0.reminderDate = fireAt }
                CoreToastManager.shared.showReminderScheduledToast(fireAt: fireAt)
            } catch {
                CoreToastManager.shared.showReminderScheduleFailedToast()
            }
        case .onCancelReminder:
            guard let bid = state.bookmarkId else { return }
            ReminderManager.cancelReminder(bookmarkId: bid)
            apply { $0.reminderDate = nil }
        case let .onExportMarkdownReady(md):
            apply { $0.lastExportMarkdown = md }
        case let .onExportPdfReady(b64):
            apply { $0.lastExportPdfBase64 = b64 }
        case let .updateLinkMetadata(
            bookmarkId,
            url,
            domain,
            videoUrl,
            audioUrl,
            metadataTitle,
            metadataDescription,
            metadataAuthor,
            metadataDate
        ):
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
    }

    /// Explicit “update readable” from the detail menu: fetch HTML, convert, insert a **new** readable version.
    private func refreshReadableFromSource() async {
        guard let bid = state.bookmarkId,
              let url = state.linkSourceUrl?.trimmingCharacters(in: .whitespacesAndNewlines),
              !url.isEmpty
        else { return }
        apply { $0.isUpdatingReadable = true; $0.lastConverterErrorMessage = nil }
        defer { apply { $0.isUpdatingReadable = false } }
        do {
            let pack = try await Unfurler.unfurl(url)
            await saveReadableBundle(bookmarkId: bid, metadata: pack.metadata, readable: pack.readable)
        } catch {
            apply { $0.lastConverterErrorMessage = String(describing: error) }
        }
    }

    /// Refresh Open Graph / link metadata and preview assets only; does **not** create a new readable version.
    private func refreshLinkMetadataOnly() async {
        guard let bid = state.bookmarkId,
              let url = state.linkSourceUrl?.trimmingCharacters(in: .whitespacesAndNewlines),
              !url.isEmpty
        else { return }
        apply { $0.isUpdatingReadable = true; $0.lastConverterErrorMessage = nil }
        defer { apply { $0.isUpdatingReadable = false } }
        do {
            let refresh = try await Unfurler.fetchMetadataAndPreviews(url)
            let meta = refresh.metadata
            if let cleaned = meta.cleanedUrl.nilIfEmpty {
                LinkmarkManager.queueCreateOrUpdateLinkDetails(
                    bookmarkId: bid,
                    url: cleaned,
                    domain: LinkmarkManager.extractDomain(from: cleaned),
                    videoUrl: meta.video,
                    audioUrl: meta.audio,
                    metadataTitle: meta.title,
                    metadataDescription: meta.description,
                    metadataAuthor: meta.author,
                    metadataDate: meta.date
                )
            }
            AllBookmarksManager.queueSetBookmarkPreviewAssets(
                bookmarkId: bid,
                imageBytes: refresh.previewImageData,
                iconBytes: refresh.previewIconData
            )
        } catch {
            apply { $0.lastConverterErrorMessage = String(describing: error) }
        }
    }

    private func saveReadableBundle(bookmarkId: String, metadata: LinkMetadataResult, readable: ReadableUnfurl) async {
        let newVersionId = UUID().uuidString
        apply { $0.selectedReadableVersionId = newVersionId }
        ReadableContentManager.queueSaveLinkReadableUnfurl(
            bookmarkId: bookmarkId,
            readableVersionId: newVersionId,
            unfurl: readable
        )
        let meta = metadata
        if let cleaned = meta.cleanedUrl.nilIfEmpty {
            LinkmarkManager.queueCreateOrUpdateLinkDetails(
                bookmarkId: bookmarkId,
                url: cleaned,
                domain: LinkmarkManager.extractDomain(from: cleaned),
                videoUrl: meta.video,
                audioUrl: meta.audio,
                metadataTitle: meta.title,
                metadataDescription: meta.description,
                metadataAuthor: meta.author,
                metadataDate: meta.date
            )
        }
        let img = await Unfurler.downloadPreviewImageBytes(urlString: meta.image)
        let logo = await Unfurler.downloadPreviewImageBytes(urlString: meta.logo)
        AllBookmarksManager.queueSetBookmarkPreviewAssets(
            bookmarkId: bookmarkId,
            imageBytes: img,
            iconBytes: logo
        )
    }
}
