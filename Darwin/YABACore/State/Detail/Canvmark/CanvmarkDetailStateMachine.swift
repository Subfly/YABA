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
            apply { $0.bookmarkId = bookmarkId }
        case let .onSave(sceneJson):
            guard let bid = state.bookmarkId else { return }
            CanvmarkManager.queueSaveCanvasSceneData(bookmarkId: bid, sceneData: Data(sceneJson.utf8))
        case .onWebInitialContentLoad, .onPickImageFromGallery, .onCaptureImageFromCamera,
             .onConsumedPendingImageInsert:
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
        case .onScheduleReminder, .onCancelReminder:
            break
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
