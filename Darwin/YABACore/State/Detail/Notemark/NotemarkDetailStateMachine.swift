//
//  NotemarkDetailStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class NotemarkDetailStateMachine: YabaBaseObservableState<NotemarkDetailUIState>, YabaScreenStateMachine {
    public override init(initialState: NotemarkDetailUIState = NotemarkDetailUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: NotemarkDetailEvent) async {
        switch event {
        case let .onInit(bookmarkId):
            let reminderDate = await ReminderManager.getPendingReminderDate(bookmarkId: bookmarkId)
            apply {
                $0.bookmarkId = bookmarkId
                $0.reminderDate = reminderDate
            }
        case let .onSave(documentJson, _):
            guard let bid = state.bookmarkId else { return }
            let data = Data(documentJson.utf8)
            NotemarkManager.queueSaveNoteDocumentData(bookmarkId: bid, documentBody: data)
            ReadableContentManager.queueSyncNotemarkReadableMirror(bookmarkId: bid, html: documentJson)
            NotemarkManager.queueCreateOrUpdateNoteDetails(bookmarkId: bid)
        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])
        case .onRequestNotificationPermission:
            _ = await ReminderManager.requestAuthorization()
            let granted = await ReminderManager.authorizationGranted()
            if !granted {
                CoreToastManager.shared.showNotificationPermissionDeniedToast()
            }
        case .onPickImageFromGallery, .onCaptureImageFromCamera:
            // When gallery/camera bridges are implemented (parity with Compose notemark detail),
            // pass selected image `Data` through `YabaImageCompression.compressDataPreservingFormat(_:)`
            // before any editor insert / `NotemarkManager.queueSaveNoteDocumentData` handoff.
            break
        case let .onWebInitialContentLoad(resultJson):
            apply { $0.webInitialContentLoadResultJson = resultJson }
        case .onConsumedInlineImageInsert:
            apply { $0.inlineImageDocumentSrc = nil }
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
        case let .onScheduleReminder(titleKey, messageKey, fireAt):
            guard let bid = state.bookmarkId else { return }
            do {
                try await ReminderManager.scheduleReminderResolvingLabel(
                    bookmarkId: bid,
                    bookmarkKindCode: BookmarkKind.note.rawValue,
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
        case let .saveDocument(bookmarkId, data):
            NotemarkManager.queueSaveNoteDocumentData(bookmarkId: bookmarkId, documentBody: data)
        case let .ensureReadableMirror(bookmarkId, json):
            ReadableContentManager.queueSyncNotemarkReadableMirror(
                bookmarkId: bookmarkId,
                html: json
            )
        }
    }
}
