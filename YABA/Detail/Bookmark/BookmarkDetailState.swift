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
    
    var folder: YabaCollection?
    var tags: [YabaCollection]
    
    init(with bookmark: Bookmark?) {
        folder = bookmark?.collections.first(where: { $0.collectionType == .folder })
        tags = bookmark?.collections.filter { $0.collectionType == .tag } ?? []
        meshColor = folder?.color.getUIColor() ?? .accentColor
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

