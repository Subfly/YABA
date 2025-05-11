//
//  BookmarkDetailState.swift
//  YABA
//
//  Created by Ali Taha on 29.04.2025.
//

import Foundation
import SwiftUI

@MainActor
@Observable
internal class BookmarkDetailState {
    var shouldShowEditBookmarkSheet = false
    var shouldShowDeleteDialog = false
    
    var meshColor: Color
    var folder: YabaCollection?
    var tags: [YabaCollection]
    
    init(with bookmark: Bookmark?) {
        let newFolder = bookmark?.collections.first(where: { $0.collectionType == .folder })
        folder = newFolder
        tags = bookmark?.collections.filter { $0.collectionType == .tag } ?? []
        meshColor = newFolder?.color.getUIColor() ?? .accentColor
    }
    
    // Only for !iOS devices
    func refresh(with newBookmarkData: Bookmark?) {
        let newFolder = newBookmarkData?.collections.first(where: { $0.collectionType == .folder })
        let newTags = newBookmarkData?.collections.filter { $0.collectionType == .tag } ?? []
        let newColor = newFolder?.color.getUIColor() ?? .accentColor
        
        withAnimation {
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
    }
    
    func onClickOpenLink(using bookmark: Bookmark?) {
        if let link = bookmark?.link,
           let url = URL(string: link) {
            if UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
            }
        }
    }
}

