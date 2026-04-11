//
//  CoreToastManager+Reminder.swift
//  YABACore
//
//  Centralized localized copy for bookmark reminder flows (matches legacy Darwin toasts).
//

import SwiftUI

extension CoreToastManager {
    /// Shown when notification permission is still denied after a request.
    public func showNotificationPermissionDeniedToast() {
        _ = show(
            message: LocalizedStringKey("Notifications Disabled Message"),
            acceptText: LocalizedStringKey("Ok"),
            iconType: .error,
            duration: .short,
            onAcceptPressed: nil
        )
    }

    /// Shown after a reminder is scheduled successfully.
    public func showReminderScheduledToast(fireAt: Date) {
        let formatted = fireAt.formatted(date: .abbreviated, time: .shortened)
        _ = show(
            message: LocalizedStringKey("Setup Reminder Success Message \(formatted)"),
            acceptText: LocalizedStringKey("Ok"),
            iconType: .success,
            duration: .short,
            onAcceptPressed: nil
        )
    }

    /// Shown when scheduling the notification failed.
    public func showReminderScheduleFailedToast() {
        _ = show(
            message: LocalizedStringKey("Data Manager Unknown Error Message"),
            acceptText: LocalizedStringKey("Ok"),
            iconType: .error,
            duration: .short,
            onAcceptPressed: nil
        )
    }
}
