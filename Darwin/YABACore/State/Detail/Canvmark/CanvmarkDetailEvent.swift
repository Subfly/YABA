//
//  CanvmarkDetailEvent.swift
//  YABACore
//

import Foundation

public enum CanvmarkDetailEvent: Sendable {
    case onInit(bookmarkId: String)
    case onSave(sceneJson: String)
    case onWebInitialContentLoad(resultJson: String?)
    case onCanvasMetricsChanged(metricsJson: String?)
    case onCanvasStyleStateChanged(styleJson: String?)
    case onToggleCanvasOptionsSheet
    case onDismissCanvasOptionsSheet
    case onPickImageFromGallery
    case onCaptureImageFromCamera
    case onConsumedPendingImageInsert
    case onDeleteBookmark(bookmarkId: String)
    case onScheduleReminder(title: String, message: String, triggerAtEpochMillis: Int64)
    case onCancelReminder
    case onExportImageReady(Data, fileExtension: String)
    case saveScene(bookmarkId: String, sceneData: Data)
}
