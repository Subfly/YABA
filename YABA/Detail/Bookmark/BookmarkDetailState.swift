//
//  BookmarkDetailState.swift
//  YABA
//
//  Created by Ali Taha on 29.04.2025.
//

import Foundation
import SwiftUI
import SwiftData

@MainActor
@Observable
internal class BookmarkDetailState {
    @ObservationIgnored
    private var unfurler: Unfurler = .init()
    
    let toastManager: ToastManager = .init()
    
    var shouldShowEditBookmarkSheet: Bool = false
    var shouldShowDeleteDialog: Bool = false
    var shouldShowShareDialog: Bool = false
    var isLoading: Bool = false
    
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
                bookmark.iconDataHolder = .init(data: fetched.iconData)
                bookmark.imageDataHolder = .init(data: fetched.imageData)
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

