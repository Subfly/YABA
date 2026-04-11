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
                documentJson: data
            )
        case .onUpdateReadableRequested, .onUpdateLinkMetadataRequested:
            await refreshLinkFromSource()
        case let .onReaderWebInitialContentLoad(resultJson):
            apply { $0.readerWebInitialLoadResultJson = resultJson }
        case let .onConverterSucceeded(result):
            guard let bid = state.bookmarkId else { return }
            await saveReadableFromConverterOutput(bookmarkId: bid, result: result)
        case let .onConverterFailed(errorMessage):
            apply { $0.lastConverterErrorMessage = errorMessage }
            YabaCoreToastManager.shared.show(
                message: LocalizedStringKey(stringLiteral: errorMessage),
                iconType: .error,
                duration: .short
            )
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
        case .onAnnotationReadableCreateCommitted, .onAnnotationReadableDeleteCommitted:
            break
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
                YabaCoreToastManager.shared.showNotificationPermissionDeniedToast()
            }
        case let .onScheduleReminder(fireAt, titleKey, messageKey):
            guard let bid = state.bookmarkId else { return }
            do {
                try await ReminderManager.scheduleReminderResolvingLabel(
                    bookmarkId: bid,
                    bookmarkKindCode: YabaCoreBookmarkKind.link.rawValue,
                    titleKey: titleKey,
                    messageKey: messageKey,
                    fireAt: fireAt
                )
                apply { $0.reminderDate = fireAt }
                YabaCoreToastManager.shared.showReminderScheduledToast(fireAt: fireAt)
            } catch {
                YabaCoreToastManager.shared.showReminderScheduleFailedToast()
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

    private func refreshLinkFromSource() async {
        guard let bid = state.bookmarkId,
              let url = state.linkSourceUrl?.trimmingCharacters(in: .whitespacesAndNewlines),
              !url.isEmpty
        else { return }
        apply { $0.isUpdatingReadable = true; $0.lastConverterErrorMessage = nil }
        defer { apply { $0.isUpdatingReadable = false } }
        do {
            let (conv, readable) = try await YabaLinkmarkUnfurlCoordinator.shared.fetchAndConvert(urlString: url)
            await saveReadableBundle(bookmarkId: bid, converter: conv, readable: readable)
        } catch {
            apply { $0.lastConverterErrorMessage = String(describing: error) }
        }
    }

    private func saveReadableFromConverterOutput(bookmarkId: String, result: YabaWebConverterResult) async {
        let readable = await DarwinConverterResultProcessor.process(
            documentJson: result.documentJson,
            assets: result.assets
        )
        await saveReadableBundle(bookmarkId: bookmarkId, converter: result, readable: readable)
    }

    private func saveReadableBundle(bookmarkId: String, converter: YabaWebConverterResult, readable: YabaReadableUnfurl) async {
        let rv = state.selectedReadableVersionId ?? UUID().uuidString
        apply { $0.selectedReadableVersionId = rv }
        ReadableContentManager.queueSaveLinkReadableUnfurl(
            bookmarkId: bookmarkId,
            readableVersionId: rv,
            unfurl: readable
        )
        let meta = converter.linkMetadata
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
        let img = await YabaUnfurler.downloadPreviewImageBytes(urlString: meta.image)
        let logo = await YabaUnfurler.downloadPreviewImageBytes(urlString: meta.logo)
        AllBookmarksManager.queueSetBookmarkPreviewAssets(
            bookmarkId: bookmarkId,
            imageBytes: img,
            iconBytes: logo
        )
    }
}
