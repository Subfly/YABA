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
    
    var meshColor: Color
    var folder: YabaCollection?
    var tags: [YabaCollection]
    
    init(with bookmark: YabaBookmark?) {
        let newFolder = bookmark?.collections.first(where: { $0.collectionType == .folder })
        folder = newFolder
        tags = bookmark?.collections.filter { $0.collectionType == .tag } ?? []
        meshColor = newFolder?.color.getUIColor() ?? .accentColor
    }
    
    // Only for !iOS devices
    func retriggerUIRefresh(with newBookmarkData: YabaBookmark?) {
        let newFolder = newBookmarkData?.collections.first(where: { $0.collectionType == .folder })
        let newTags = newBookmarkData?.collections.filter { $0.collectionType == .tag } ?? []
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
    }
    
    func onNotificationPermissionRequested(
        onSuccessCalback: @escaping () -> Void
    ) {
        UNUserNotificationCenter
            .current()
            .requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] success, error in
                if success {
                    onSuccessCalback()
                } else if error != nil {
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
        let content = UNMutableNotificationContent()
        content.title = "Feed the cat"
        content.subtitle = "It looks hungry"
        content.sound = UNNotificationSound.default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 5, repeats: false)
        let request = UNNotificationRequest(
            identifier: bookmark.bookmarkId,
            content: content,
            trigger: trigger
        )

        UNUserNotificationCenter.current().add(request)
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
                bookmark.iconDataHolder = .init(data: fetched.iconData)
                bookmark.imageDataHolder = .init(data: fetched.imageData)
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

