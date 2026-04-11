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
            NotemarkManager.queueSaveNoteDocumentData(bookmarkId: bid, documentBody: Data(documentJson.utf8))
            let versionId = UUID().uuidString
            ReadableContentManager.queueSyncNotemarkReadableMirror(bookmarkId: bid, versionId: versionId, documentJson: documentJson)
            NotemarkManager.queueCreateOrUpdateNoteDetails(bookmarkId: bid, readableVersionId: versionId)
        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])
        case .onRequestNotificationPermission:
            _ = await ReminderManager.requestAuthorization()
        case .onPickImageFromGallery, .onCaptureImageFromCamera,
             .onWebInitialContentLoad:
            break
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
                    bookmarkKindCode: YabaCoreBookmarkKind.note.rawValue,
                    titleKey: titleKey,
                    messageKey: messageKey,
                    fireAt: fireAt
                )
                apply { $0.reminderDate = fireAt }
            } catch {}
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
        case let .ensureReadableMirror(bookmarkId, versionId, json):
            ReadableContentManager.queueSyncNotemarkReadableMirror(
                bookmarkId: bookmarkId,
                versionId: versionId,
                documentJson: json
            )
        }
    }
}
