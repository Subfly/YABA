//
//  CollectionDetailState.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import Foundation
import SwiftUI
import SwiftData

@MainActor
@Observable
internal class CollectionDetailState {
    var searchQuery: String = ""
    var shouldShowCreateBookmarkSheet: Bool = false
    var shouldShowMoveBookmarksSheet: Bool = false
    var shouldShowDeleteDialog: Bool = false
    
    var isInSelectionMode: Bool = false {
        didSet {
            if !isInSelectionMode {
                selectedBookmarks = []
            }
        }
    }
    var selectedBookmarks: Set<YabaBookmark> = []
    var selectedFolderToMove: YabaCollection? = nil
    
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
    
    func handleChangeFolderRequest(with modelContext: ModelContext) {
        guard let selectedFolderToMove else {
            return
        }
        
        selectedBookmarks.forEach { bookmark in
            bookmark.collections?.removeAll { collection in
                collection.collectionType == .folder
            }
            bookmark.collections?.append(selectedFolderToMove)
        }
        
        isInSelectionMode = false
        
        try? modelContext.save()
    }
    
    func handleDeletionRequest(with modelContext: ModelContext) {
        selectedBookmarks.forEach { bookmark in
            modelContext.delete(bookmark)
        }
        
        isInSelectionMode = false
        
        try? modelContext.save()
    }
}
