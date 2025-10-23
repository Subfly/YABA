//
//  CollectionDropDelegate.swift
//  YABA
//
//  Created by Ali Taha on 23.10.2025.
//

import SwiftUI
import UniformTypeIdentifiers

internal struct CollectionDropDelegate: DropDelegate {
    @Binding var itemState: CollectionItemState
    let targetCollection: YabaCollection
    let onDropDone: () -> Void

    func dropEntered(info: DropInfo) {
        updateDropZone(info: info)
    }

    func dropUpdated(info: DropInfo) -> DropProposal? {
        updateDropZone(info: info)
        return DropProposal(operation: .move)
    }

    func dropExited(info: DropInfo) {
        withAnimation(.smooth) {
            itemState.dropZone = .none
        }
    }

    func performDrop(info: DropInfo) -> Bool {
        // decode the actual transferable here if needed
        withAnimation(.smooth) {
            itemState.dropZone = .none
        }
        onDropDone()
        return true
    }

    // MARK: - Live hover logic
    private func updateDropZone(info: DropInfo) {
        let y = info.location.y

        withAnimation(.smooth) {
            // Bookmarks: always middle
            if info.hasItemsConforming(to: [.yabaBookmark]) {
                itemState.dropZone = .middle
                return
            }

            // Collections: allow top/middle/bottom only if valid type combination
            if info.hasItemsConforming(to: [.yabaCollection]) {
                // Determine dragged type from type hint (optional, just rely on UI rules)
                // Folders can go into folders, tags only top/bottom
                switch targetCollection.collectionType {
                case .folder:
                    itemState.dropZone = calculateFolderDropZone(y)
                case .tag:
                    if y < 15 {
                        itemState.dropZone = .top
                    } else if y > 40 {
                        itemState.dropZone = .bottom
                    } else {
                        itemState.dropZone = .none
                    }
                }
                return
            }

            // fallback: no change
            itemState.dropZone = .none
        }
    }

    private func calculateFolderDropZone(_ y: CGFloat) -> DropZone {
        if y < 15 {
            return .top
        } else if y > 40 {
            return .bottom
        } else {
            return .middle
        }
    }
}
