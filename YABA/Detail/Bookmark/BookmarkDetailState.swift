//
//  BookmarkDetailState.swift
//  YABA
//
//  Created by Ali Taha on 29.04.2025.
//

import Foundation
import SwiftUI
import SwiftData

internal enum DetailMode {
    case detail, reader
}

@MainActor
@Observable
internal class BookmarkDetailState {
    @ObservationIgnored
    private var unfurler: Unfurler = .init()
    
    let toastManager: ToastManager = .init()
    
    var shouldShowEditBookmarkSheet: Bool = false
    var shouldShowTimePicker: Bool = false
    var shouldShowDeleteDialog: Bool = false
    var shouldShowShareDialog: Bool = false
    var isLoading: Bool = false
    
    var currentMode: DetailMode = .detail
    var selectedReminderDate: Date?
    
    var meshColor: Color
    var reminderDate: Date?
    var folder: YabaCollection?
    var tags: [YabaCollection]
    
    init(with bookmark: YabaBookmark?) {
        let newFolder = bookmark?.collections?.first(where: { $0.collectionType == .folder })
        folder = newFolder
        tags = bookmark?.collections?.filter { $0.collectionType == .tag } ?? []
        meshColor = newFolder?.color.getUIColor() ?? .accentColor
        
        UNUserNotificationCenter.current().getPendingNotificationRequests { [weak self] requests in
            if let request = requests.first(where: { $0.identifier == bookmark?.bookmarkId }),
               let calendarTrigger = request.trigger as? UNCalendarNotificationTrigger {
                let triggerDateComponents = calendarTrigger.dateComponents
                if let fireDate = Calendar.current.date(from: triggerDateComponents) {
                    self?.reminderDate = fireDate
                }
            }
        }
    }
    
    // Only for !iOS devices
    func retriggerUIRefresh(with newBookmarkData: YabaBookmark?) {
        let newFolder = newBookmarkData?.collections?.first(where: { $0.collectionType == .folder })
        let newTags = newBookmarkData?.collections?.filter { $0.collectionType == .tag } ?? []
        let newColor = newFolder?.color.getUIColor() ?? .accentColor
        
        if newFolder?.id != folder?.id {
            folder = newFolder
        }
        
        if !tags.elementsEqual(newTags) {
            tags = newTags
        }
        
        if meshColor != newColor {
            meshColor = newColor
        }
        
        UNUserNotificationCenter.current().getPendingNotificationRequests { [weak self] requests in
            if let request = requests.first(where: { $0.identifier == newBookmarkData?.bookmarkId }),
               let calendarTrigger = request.trigger as? UNCalendarNotificationTrigger {
                let triggerDateComponents = calendarTrigger.dateComponents
                if let fireDate = Calendar.current.date(from: triggerDateComponents) {
                    self?.reminderDate = fireDate
                }
            }
        }
        
        selectedReminderDate = nil
    }
    
    func onNotificationPermissionRequested(
        onSuccessCalback: @escaping () -> Void,
        onDeclineCallback: @escaping () -> Void
    ) {
        UNUserNotificationCenter
            .current()
            .requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] success, error in
                if success {
                    onSuccessCalback()
                } else if error != nil {
                    onDeclineCallback()
                    self?.toastManager.show(
                        message: LocalizedStringKey("Notifications Disabled Message"),
                        accentColor: .red,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .error,
                        onAcceptPressed: { self?.toastManager.hide() }
                    )
                }
        }
    }
    
    func addRemindMe(to bookmark: YabaBookmark) {
        guard let selectedReminderDate else { return }
        
        onRemoveReminder(from: bookmark)
        
        let notificationTitle = [
            "Notification Title 1",
            "Notification Title 2",
            "Notification Title 3",
            "Notification Title 4",
            "Notification Title 5"
        ].randomElement() ?? "Notification Title 1"
        
        let subtitleKey = [
            "Notification Message 1 %@",
            "Notification Message 2 %@",
            "Notification Message 3 %@",
            "Notification Message 4 %@",
            "Notification Message 5 %@"
        ].randomElement() ?? "Notification Message 1 %@"
        let notificationSubtitle = String.localizedStringWithFormat(
            String(localized: .init(subtitleKey)),
            bookmark.label
        )
        
        let content = UNMutableNotificationContent()
        content.title = String(localized: .init(notificationTitle))
        content.subtitle = String(localized: .init(notificationSubtitle))
        content.sound = UNNotificationSound.default
        content.interruptionLevel = .timeSensitive
        content.userInfo = [
            "id": bookmark.bookmarkId
        ]
        
        let triggerDate = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: selectedReminderDate
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: triggerDate, repeats: false)
        
        let request = UNNotificationRequest(
            identifier: bookmark.bookmarkId,
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request)
        
        withAnimation {
            reminderDate = selectedReminderDate
        }
        let formattedDate = selectedReminderDate.formatted(date: .abbreviated, time: .shortened)
        toastManager.show(
            message: LocalizedStringKey("Setup Reminder Success Message \(formattedDate)"),
            accentColor: .green,
            acceptText: LocalizedStringKey("Ok"),
            iconType: .success,
            onAcceptPressed: { self.toastManager.hide() }
        )
    }
    
    func onRemoveReminder(from bookmark: YabaBookmark) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(
            withIdentifiers: [bookmark.bookmarkId]
        )
        reminderDate = nil
    }
    
    func changeMode() {
        withAnimation {
            currentMode = switch currentMode {
            case .detail: .reader
            case .reader: .detail
            }
        }
    }
    
    func onClickOpenLink(using bookmark: YabaBookmark?) {
        if let link = bookmark?.link,
           let url = URL(string: link) {
            if UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
            }
        }
    }
    
    func refetchData(with bookmark: YabaBookmark, using modelContext: ModelContext) async {
        isLoading = true
        defer { isLoading = false }
        
        do {
            if let fetched = try await unfurler.unfurl(urlString: bookmark.link) {
                bookmark.domain = fetched.host ?? ""
                if bookmark.label.isEmpty {
                    bookmark.label = fetched.title ?? ""
                }
                if bookmark.bookmarkDescription.isEmpty {
                    bookmark.bookmarkDescription = fetched.description ?? ""
                }
                bookmark.iconUrl = fetched.iconURL
                bookmark.imageUrl = fetched.imageURL
                bookmark.videoUrl = fetched.videoURL
                bookmark.readableHTML = fetched.readableHTML
                bookmark.iconDataHolder = fetched.iconData
                bookmark.imageDataHolder = fetched.imageData
                bookmark.editedAt = .now
            }
            
            try modelContext.save()
            
            toastManager.show(
                message: LocalizedStringKey("Generic Unfurl Success Text"),
                accentColor: .green,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .success,
                onAcceptPressed: { self.toastManager.hide() }
            )
        } catch UnfurlError.cannotCreateURL(let message) {
            toastManager.show(
                message: message,
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
        } catch UnfurlError.unableToUnfurl(let message) {
            toastManager.show(
                message: message,
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
        } catch {
            toastManager.show(
                message: LocalizedStringKey("Generic Unfurl Error Text"),
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
        }
    }
}

