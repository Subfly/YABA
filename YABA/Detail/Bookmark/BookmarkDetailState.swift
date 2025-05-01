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
    
    func onClickOpenLink(using bookmark: Bookmark?) {
        if let link = bookmark?.link,
           let url = URL(string: link) {
            if UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
            }
        }
    }
}

