//
//  ImagemarkDetailStateMachine.swift
//  YABACore
//

import Foundation
import Photos
import UIKit
import SwiftUI

@MainActor
public final class ImagemarkDetailStateMachine: YabaBaseObservableState<ImagemarkDetailUIState>, YabaScreenStateMachine {
    public override init(initialState: ImagemarkDetailUIState = ImagemarkDetailUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: ImagemarkDetailEvent) async {
        switch event {
        case let .onInit(bookmarkId):
            AllBookmarksManager.queueRecordBookmarkView(bookmarkId: bookmarkId)
            let reminderDate = await ReminderManager.getPendingReminderDate(bookmarkId: bookmarkId)
            apply {
                $0.bookmarkId = bookmarkId
                $0.reminderDate = reminderDate
            }
        case let .onDeleteBookmark(bookmarkId):
            AllBookmarksManager.queueDeleteBookmarks(bookmarkIds: [bookmarkId])
        case .onShareImage:
            await handleShareImage()
        case .onExportImage:
            await handleExportImage()
        case .onConsumePendingShare:
            if let url = state.pendingShareFileURL {
                try? FileManager.default.removeItem(at: url)
            }
            apply { $0.pendingShareFileURL = nil }
        case .onRequestNotificationPermission:
            _ = await ReminderManager.requestAuthorization()
            let granted = await ReminderManager.authorizationGranted()
            if !granted {
                CoreToastManager.shared.showNotificationPermissionDeniedToast()
            }
        case let .onScheduleReminder(titleKey, messageKey, fireAt):
            guard let bid = state.bookmarkId else { return }
            do {
                try await ReminderManager.scheduleReminderResolvingLabel(
                    bookmarkId: bid,
                    bookmarkKindCode: BookmarkKind.image.rawValue,
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
        case let .updateSummary(bookmarkId, summary):
            ImagemarkManager.queueCreateOrUpdateImageDetails(bookmarkId: bookmarkId, summary: summary)
        }
    }

    private func handleShareImage() async {
        guard let bid = state.bookmarkId else { return }
        if let old = state.pendingShareFileURL {
            try? FileManager.default.removeItem(at: old)
        }
        apply { $0.pendingShareFileURL = nil }
        do {
            guard let payload = try await ImagemarkManager.fetchExportPayload(bookmarkId: bid) else {
                CoreToastManager.shared.show(
                    message: "Bookmark Detail Image Error Title",
                    iconType: .error,
                    duration: .short
                )
                return
            }
            let base = MarkdownExportSupport.sanitizeBaseFolderName(payload.label, emptyFallback: "image")
            let url = FileManager.default.temporaryDirectory
                .appendingPathComponent("YABA-\(base)-\(UUID().uuidString.prefix(8)).jpg")
            try payload.imageData.write(to: url, options: .atomic)
            apply { $0.pendingShareFileURL = url }
        } catch {
            CoreToastManager.shared.show(
                message: LocalizedStringKey(""),
                iconType: .error,
                duration: .short
            )
        }
    }

    private func handleExportImage() async {
        guard let bid = state.bookmarkId else { return }
        do {
            guard let payload = try await ImagemarkManager.fetchExportPayload(bookmarkId: bid) else {
                CoreToastManager.shared.show(
                    message: "Bookmark Detail Image Error Title",
                    iconType: .error,
                    duration: .short
                )
                return
            }
            let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
            guard status == .authorized || status == .limited else {
                CoreToastManager.shared.showNotificationPermissionDeniedToast()
                return
            }
            guard let image = UIImage(data: payload.imageData) else {
                CoreToastManager.shared.show(
                    message: "Bookmark Detail Image Error Title",
                    iconType: .error,
                    duration: .short
                )
                return
            }
            try await PHPhotoLibrary.shared().performChanges {
                PHAssetChangeRequest.creationRequestForAsset(from: image)
            }
            CoreToastManager.shared.show(
                message: LocalizedStringKey("Bookmark Detail Saved to Photos Message"),
                iconType: .success,
                duration: .short
            )
        } catch {
            CoreToastManager.shared.show(
                message: "",
                iconType: .error,
                duration: .short
            )
        }
    }
}
