//
//  CanvmarkDetailStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class CanvmarkDetailStateMachine: YabaBaseObservableState<CanvmarkDetailUIState>, YabaScreenStateMachine {
    public override init(initialState: CanvmarkDetailUIState = CanvmarkDetailUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: CanvmarkDetailEvent) async {
        switch event {
        case let .onInit(bookmarkId):
            let reminderDate = await ReminderManager.getPendingReminderDate(bookmarkId: bookmarkId)
            apply {
                $0.bookmarkId = bookmarkId
                $0.reminderDate = reminderDate
            }
        case let .onSave(sceneJson):
            guard let bid = state.bookmarkId else { return }
            CanvmarkManager.queueSaveCanvasSceneData(bookmarkId: bid, sceneData: Data(sceneJson.utf8))
        case let .onWebInitialContentLoad(resultJson):
            apply { $0.webInitialContentLoadResultJson = resultJson }
        case .onPickImageFromGallery, .onCaptureImageFromCamera,
             .onConsumedPendingImageInsert:
            // When native pickers are implemented (parity with Compose canvmark detail), compress
            // `Data` with `YabaImageCompression.compressDataPreservingFormat(_:)` before sending to the canvas.
            break
        case let .onCanvasMetricsChanged(metricsJson):
            apply { $0.metricsJson = metricsJson }
        case let .onCanvasStyleStateChanged(styleJson):
            apply { $0.styleJson = styleJson }
        case .onToggleCanvasOptionsSheet:
            apply { $0.canvasOptionsSheetOpen.toggle() }
        case .onDismissCanvasOptionsSheet:
            apply { $0.canvasOptionsSheetOpen = false }
        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])
        case let .onScheduleReminder(titleKey, messageKey, fireAt):
            guard let bid = state.bookmarkId else { return }
            do {
                try await ReminderManager.scheduleReminderResolvingLabel(
                    bookmarkId: bid,
                    bookmarkKindCode: BookmarkKind.canvas.rawValue,
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
        case let .onExportImageReady(data, ext):
            apply {
                $0.pendingExportImageData = data
                $0.pendingExportImageExtension = ext
            }
        case let .saveScene(bookmarkId, sceneData):
            CanvmarkManager.queueSaveCanvasSceneData(bookmarkId: bookmarkId, sceneData: sceneData)
        }
    }
}
