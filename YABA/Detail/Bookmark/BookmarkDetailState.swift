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
    var meshColor: Color = .accentColor
    var shouldShowEditBookmarkSheet = false
    var shouldShowDeleteDialog = false
    
    var folder: YabaCollection? = nil
    var tags: [YabaCollection] = []
    
    func initialize(with bookmark: Bookmark?) {
        let newfolder = bookmark?.collections.first(where: { $0.collectionType == .folder })
        let newTags = bookmark?.collections.filter { $0.collectionType == .tag } ?? []
        let newColor = folder?.color.getUIColor() ?? .accentColor
        
        withAnimation {
            if newfolder?.id != folder?.id {
                folder = newfolder
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

