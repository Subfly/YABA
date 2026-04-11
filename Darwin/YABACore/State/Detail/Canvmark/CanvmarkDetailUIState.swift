//
//  CanvmarkDetailUIState.swift
//  YABACore
//

import Foundation

public struct CanvmarkDetailUIState: Sendable {
    public var bookmarkId: String?
    public var canvasOptionsSheetOpen: Bool
    public var metricsJson: String?
    public var styleJson: String?
    public var pendingExportImageData: Data?
    public var pendingExportImageExtension: String
    public var reminderDate: Date?

    public init(
        bookmarkId: String? = nil,
        canvasOptionsSheetOpen: Bool = false,
        metricsJson: String? = nil,
        styleJson: String? = nil,
        pendingExportImageData: Data? = nil,
        pendingExportImageExtension: String = "png",
        reminderDate: Date? = nil
    ) {
        self.bookmarkId = bookmarkId
        self.canvasOptionsSheetOpen = canvasOptionsSheetOpen
        self.metricsJson = metricsJson
        self.styleJson = styleJson
        self.pendingExportImageData = pendingExportImageData
        self.pendingExportImageExtension = pendingExportImageExtension
        self.reminderDate = reminderDate
    }
}
