//
//  ImagemarkDetailEvent.swift
//  YABACore
//

import Foundation

public enum ImagemarkDetailEvent: Sendable {
    case onInit(bookmarkId: String)
    case onDeleteBookmark(bookmarkId: String)
    case onShareImage
    case onExportImage
    case onConsumePendingShare
    case onRequestNotificationPermission
    case onScheduleReminder(titleKey: String, messageKey: String, fireAt: Date)
    case onCancelReminder

    case updateSummary(bookmarkId: String, summary: String?)
}
