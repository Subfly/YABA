//
//  ReminderManager.swift
//  YABACore
//
//  Central Darwin bookmark reminders via `UNUserNotificationCenter`.
//  Notification identifier = `bookmarkId`; `userInfo["id"]` = bookmark id (deep link parity).
//
//  Title and message use **localization keys** resolved on iOS via `Bundle` (default `.main`).
//
//  User-visible toasts for permission / schedule outcomes are emitted by detail state machines
//  (see `CoreToastManager`) rather than from this enum.
//

import Foundation
import SwiftData
import UserNotifications

public enum ReminderManager {
    // MARK: - Permission

    public static func requestAuthorization() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
        } catch {
            return false
        }
    }

    /// Whether the user can receive alerts (authorized or provisional).
    public static func authorizationGranted() async -> Bool {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        default:
            return false
        }
    }

    // MARK: - Read

    /// Pending fire date, or `nil` if none or if the scheduled time is in the past (request removed).
    public static func getPendingReminderDate(bookmarkId: String) async -> Date? {
        await withCheckedContinuation { continuation in
            UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
                guard let request = requests.first(where: { $0.identifier == bookmarkId }),
                      let trigger = request.trigger as? UNCalendarNotificationTrigger
                else {
                    continuation.resume(returning: nil)
                    return
                }
                let components = trigger.dateComponents
                guard let fireDate = Calendar.current.date(from: components) else {
                    continuation.resume(returning: nil)
                    return
                }
                if fireDate <= Date() {
                    UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [bookmarkId])
                    continuation.resume(returning: nil)
                } else {
                    continuation.resume(returning: fireDate)
                }
            }
        }
    }

    // MARK: - Write

    /// Schedules a reminder using localized title and body template from `titleKey` / `messageKey`.
    /// - Parameters:
    ///   - bundle: Strings bundle (default: app’s main bundle). Use a framework bundle when keys live in the core module.
    ///   - table: Optional `.strings` table name; `nil` uses the default Localizable table.
    public static func scheduleReminder(
        bookmarkId: String,
        bookmarkKindCode: Int,
        titleKey: String,
        messageKey: String,
        bookmarkLabel: String,
        fireAt: Date,
        bundle: Bundle = .main,
        table: String? = nil
    ) async throws {
        cancelReminder(bookmarkId: bookmarkId)

        let title = localizedString(key: titleKey, bundle: bundle, table: table)
        let messageTemplate = localizedString(key: messageKey, bundle: bundle, table: table)

        let content = UNMutableNotificationContent()
        content.title = title
        content.subtitle = formatLocalizedMessageTemplate(messageTemplate, bookmarkLabel: bookmarkLabel)
        content.sound = .default
        content.interruptionLevel = .timeSensitive
        content.userInfo = [
            "id": bookmarkId,
            "bookmarkKindCode": bookmarkKindCode,
        ]

        let triggerDate = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: fireAt
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: triggerDate, repeats: false)
        let request = UNNotificationRequest(identifier: bookmarkId, content: content, trigger: trigger)
        try await UNUserNotificationCenter.current().add(request)
    }

    /// Like ``scheduleReminder`` but loads `bookmarkLabel` from SwiftData for `%@` / `%s` in the localized message.
    public static func scheduleReminderResolvingLabel(
        bookmarkId: String,
        bookmarkKindCode: Int,
        titleKey: String,
        messageKey: String,
        fireAt: Date,
        bundle: Bundle = .main,
        table: String? = nil
    ) async throws {
        let label = resolveBookmarkLabel(bookmarkId: bookmarkId)
        try await scheduleReminder(
            bookmarkId: bookmarkId,
            bookmarkKindCode: bookmarkKindCode,
            titleKey: titleKey,
            messageKey: messageKey,
            bookmarkLabel: label,
            fireAt: fireAt,
            bundle: bundle,
            table: table
        )
    }

    public static func cancelReminder(bookmarkId: String) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [bookmarkId])
    }

    public static func cancelReminders(bookmarkIds: [String]) {
        guard !bookmarkIds.isEmpty else { return }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: bookmarkIds)
    }

    public static func cancelAllReminders() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
    }

    // MARK: - Private

    private static func localizedString(key: String, bundle: Bundle, table: String?) -> String {
        bundle.localizedString(forKey: key, value: key, table: table)
    }

    private static func resolveBookmarkLabel(bookmarkId: String) -> String {
        guard let context = try? CoreStore.makeWriteContext() else { return "" }
        return (try? YabaCorePersistenceHelpers.bookmark(bookmarkId: bookmarkId, context: context))?.label ?? ""
    }

    /// Applies bookmark label to a localized template (supports `%@`, `%s`, `%d` is not used).
    private static func formatLocalizedMessageTemplate(_ localizedTemplate: String, bookmarkLabel: String) -> String {
        guard localizedTemplate.contains("%") else { return localizedTemplate }
        if localizedTemplate.contains("%s") {
            return localizedTemplate.replacingOccurrences(of: "%s", with: bookmarkLabel)
        }
        return String(format: localizedTemplate, locale: Locale.current, arguments: [bookmarkLabel])
    }
}
