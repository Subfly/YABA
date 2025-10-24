//
//  CollectionDetailState.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import Foundation
import SwiftUI

@MainActor
@Observable
internal class CollectionDetailState {
    var searchQuery: String = ""
    var shouldShowCreateBookmarkSheet: Bool = false
    var shouldShowMoveBookmarksSheet: Bool = false
    
    var isInSelectionMode: Bool = false {
        didSet {
            if !isInSelectionMode {
                selectedBookmarks = []
            }
        }
    }
    var selectedBookmarks: Set<YabaBookmark> = []
    
    func upsertBookmarkInSelections(_ bookmark: YabaBookmark) {
        if selectedBookmarks.contains(bookmark) {
            _ = withAnimation {
                selectedBookmarks.remove(bookmark)
            }
        } else {
            _ = withAnimation {
                selectedBookmarks.insert(bookmark)
            }
        }
    }
}
