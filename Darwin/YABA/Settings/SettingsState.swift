// ARCHIVED: Previous implementation preserved below (not compiled). UI rebuild in progress.

#if false
//
//  SettingsState.swift
//  YABA
//
//  Created by Ali Taha on 23.05.2025.
//

import SwiftUI
import SwiftData
import UserNotifications
import WidgetKit

internal enum SettingsNavigationDestination: Hashable {
    case previousAnnouncements, logs
}

@MainActor
@Observable
internal class SettingsState {
    var settingsNavPath: [SettingsNavigationDestination] = []

    var shouldShowGuideSheet: Bool = false
    var showDeleteAllDialog: Bool = false
    var isDeleting: Bool = false

    func deleteAllData(
        using modelContext: ModelContext,
        onFinishCallback: @escaping () -> Void
    ) {
        isDeleting = true
        defer {
            showDeleteAllDialog = false
            isDeleting = false
        }

        Task { @MainActor in
            UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
            UNUserNotificationCenter.current().removeAllDeliveredNotifications()

            try? YabaDataLogger.shared.logBulkDelete(shouldSave: false)

            try? modelContext.delete(model: YabaBookmark.self)
            try? await Task.sleep(for: .seconds(1))

            let descriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate { _ in true }
            )
            if let collections = try? modelContext.fetch(descriptor) {
                collections.forEach { collection in
                    modelContext.delete(collection)
                }
                try? await Task.sleep(for: .seconds(1))
            }

            try? modelContext.save()

            WidgetCenter.shared.reloadAllTimelines()
            onFinishCallback()
        }
    }

    func onNavigateToAnnouncements() {
        settingsNavPath.append(.previousAnnouncements)
    }

    func onNavigateToLogs() {
        settingsNavPath.append(.logs)
    }

    func onResetAppStorage() {
        @AppStorage(Constants.hasPassedOnboardingKey)
        var hasPassedOnboarding = false

        @AppStorage(Constants.preferredThemeKey)
        var theme: ThemePreference = .system

        @AppStorage(Constants.preferredContentAppearanceKey)
        var contentAppearance: ContentAppearance = .list

        @AppStorage(Constants.preferredCardImageSizingKey)
        var imageSizing: CardImageSizing = .small

        @AppStorage(Constants.preferredCollectionSortingKey)
        var collectionSortType: SortType = .createdAt

        @AppStorage(Constants.preferredBookmarkSortingKey)
        var bookmarkSortType: SortType = .createdAt

        @AppStorage(Constants.preferredSortOrderKey)
        var sortOrderType: SortOrderType = .ascending

        @AppStorage(Constants.announcementsYaba1_2UpdateKey)
        var showUpdateAnnouncement: Bool = true

        @AppStorage(Constants.announcementsCloudKitDropKey)
        var showCloudKitAnnouncement: Bool = true

        @AppStorage(Constants.hasNamedDeviceKey)
        var hasNamedDevice: Bool = false

        @AppStorage(Constants.deviceNameKey)
        var deviceName: String = ""

        hasPassedOnboarding = false
        showUpdateAnnouncement = true
        showCloudKitAnnouncement = true
        hasNamedDevice = false
        theme = .system
        contentAppearance = .list
        imageSizing = .small
        collectionSortType = .createdAt
        bookmarkSortType = .createdAt
        sortOrderType = .ascending
        deviceName = ""
    }
}

#endif
