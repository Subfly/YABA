//
//  CollectionItemState.swift
//  YABA
//
//  Created by Ali Taha on 23.10.2025.
//

import SwiftUI
import UniformTypeIdentifiers

@MainActor
@Observable
internal class CollectionItemState {
    var isHovered: Bool = false
    var isTargeted: Bool = false // Used for list style targeting
    var isExpanded: Bool = false
    var dropZone: DropZone = .none // Used for home view targeting
    var shouldShowDeleteDialog: Bool = false
    var shouldShowEditSheet: Bool = false
    var shouldShowMenuItems: Bool = false
    var shouldShowCreateBookmarkSheet: Bool = false
    
    /**
     * Should be used only for list items. Home item's have their own access to specific functions directly.
     */
    func onDropTakeAction(
        providers: [NSItemProvider],
        onMoveFolder: @escaping (String) -> Void,
        onMoveBookmark: @escaping (String) -> Void
    ) -> Bool {
        guard let provider = providers.first else { return false }

        if provider.hasItemConformingToTypeIdentifier(UTType.yabaCollection.identifier) {
            // It's a collection
            _ = provider.loadTransferable(type: YabaCodableCollection.self) { result in
                switch result {
                case .success(let item):
                    Task { @MainActor in
                        onMoveFolder(item.collectionId)
                    }
                case .failure:
                    break
                }
            }
        } else if provider.hasItemConformingToTypeIdentifier(UTType.yabaBookmark.identifier) {
            // It's a bookmark
            _ = provider.loadTransferable(type: YabaCodableBookmark.self) { result in
                switch result {
                case .success(let bookmark):
                    Task { @MainActor in
                        if let bookmarkId = bookmark.bookmarkId {
                            onMoveBookmark(bookmarkId)
                        }
                    }
                case .failure:
                    break
                }
            }
        } else {
            return false
        }

        return true
    }
    
    func onDropBookmarkTakeAction(
        providers: [NSItemProvider],
        onMoveBookmark: @escaping (String) -> Void
    ) {
        guard let provider = providers.first else { return }
        
        if provider.hasItemConformingToTypeIdentifier(UTType.yabaBookmark.identifier) {
            _ = provider.loadTransferable(type: YabaCodableBookmark.self) { result in
                switch result {
                case .success(let bookmark):
                    Task { @MainActor in
                        if let bookmarkId = bookmark.bookmarkId {
                            onMoveBookmark(bookmarkId)
                        }
                    }
                case .failure:
                    break
                }
            }
        }
    }
    
    func onDropFolderTakeAction(
        providers: [NSItemProvider],
        onMoveFolder: @escaping (String) -> Void,
    ) {
        guard let provider = providers.first else { return }

        if provider.hasItemConformingToTypeIdentifier(UTType.yabaCollection.identifier) {
            // It's a collection
            _ = provider.loadTransferable(type: YabaCodableCollection.self) { result in
                switch result {
                case .success(let item):
                    Task { @MainActor in
                        onMoveFolder(item.collectionId)
                    }
                case .failure:
                    break
                }
            }
        }
    }
}
