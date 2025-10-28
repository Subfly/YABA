//
//  MoveManager.swift
//  YABA
//
//  Created by Ali Taha on 12.10.2025.
//

import Foundation
import SwiftUI
import SwiftData

@MainActor
@Observable
@preconcurrency
class MoveManager {
    private let modelContext: ModelContext = YabaModelContainer.getContext()
    
    func onMoveBookmark(
        bookmarkID: String,
        toCollectionID: String,
    ) {
        guard let bookmark: YabaBookmark = try? modelContext.fetch(
            FetchDescriptor<YabaBookmark>(
                predicate: #Predicate {
                    $0.bookmarkId == bookmarkID
                }
            )
        ).first else { return }
        
        guard let collection: YabaCollection = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == toCollectionID
                }
            )
        ).first else { return }
        
        // MARK TAG INTERACTIONS
        if collection.collectionType == .tag {
            if collection.bookmarks?.contains(bookmark) == true {
                return
            }
            
            // Add tag to bookmark
            do {
                bookmark.version += 1
                collection.version += 1
                collection.bookmarks?.append(bookmark)
                try modelContext.save()
                return
            } catch {
                return
            }
        }
        
        // MARK FOLDER INTERACTIONS
        if collection.collectionType == .folder {
            // MOVE BOOKMARK TO SAME FOLDER, SKIP
            if collection.bookmarks?.contains(bookmark) == true {
                return
            }
            
            // REMOVE BOOKMARK FROM OLD PARENT
            guard let oldParent = bookmark.getParentFolder() else { return }
            
            oldParent.bookmarks?.removeAll { innerBookmark in
                innerBookmark.bookmarkId == bookmark.bookmarkId
            }
            oldParent.version += 1
            
            // MOVE BOOKMARK TO NEW PARENT
            bookmark.version += 1
            collection.bookmarks?.append(bookmark)
            collection.version += 1
            
            do {
                try modelContext.save()
            } catch {
                return
            }
        }
    }
    
    func onMoveFolder(
        from folder1ID: String,
        to folder2ID: String
    ) {
        // MOVE CHILDREN TO SELF, SKIP
        if folder1ID == folder2ID {
            return
        }
        
        guard let folder1: YabaCollection = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == folder1ID
                }
            )
        ).first else { return }
        
        guard let folder2: YabaCollection = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == folder2ID
                }
            )
        ).first else { return }
        
        // MOVE Folder to Tag - Tag to Folder - Tag to Tag, SKIP
        if folder1.collectionType == .tag || folder2.collectionType == .tag {
            return
        }
        
        // MOVE CHILDREN TO IT'S PARENT AGAIN, SKIP
        if folder2.children.contains(where: { $0.collectionId == folder1ID }) {
            return
        }
        
        // MOVE PARENT TO CHILDREN IS ILLEGAL, SKIP
        if folder1.getDescendants().contains(where: { $0.collectionId == folder2ID }) {
            return
        }
        
        do {
            // 1. Remove from old parent's children and reorder them
            if let oldParent = folder1.parent {
                oldParent.children.removeAll(where: { $0.collectionId == folder1ID })
                
                let sortedOld = sortSiblings(oldParent.children)
                for (index, item) in sortedOld.enumerated() {
                    item.order = index
                    item.version += 1
                }
                oldParent.children = sortedOld
                oldParent.version += 1
            }

            // 2. Add to new parent's children
            folder1.parent = folder2
            folder2.children.append(folder1)

            // 3. Determine new order
            if folder2.children.count == 1 {
                folder1.order = 0 // first child
            } else {
                let maxOrder = folder2.children.map { $0.order }.max() ?? -1
                folder1.order = maxOrder + 1
            }

            // 4. Normalize numbering in new parent's children
            let sortedNew = sortSiblings(folder2.children)
            for (index, item) in sortedNew.enumerated() {
                item.order = index
                item.version += 1
            }
            folder2.children = sortedNew
            
            folder1.version += 1
            folder2.version += 1
            
            try modelContext.save()
        } catch {
            return
        }
    }
    
    func onCustomSortCollections() {
        // Fetch all collections
        guard let allCollections: [YabaCollection] = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>()
        ) else { return }
        
        // 1) Handle folders: find root folders (parent == nil) and assign 0..n-1
        let allFolders = allCollections.filter { $0.collectionType == .folder }
        let rootFolders = allFolders.filter { $0.parent == nil }
        assignPerGroupOrders(rootFolders)
        
        // 2) Handle tags (flat list) - assign 0..tags.count-1
        let tags = allCollections.filter { $0.collectionType == .tag }
        let sortedTags = sortSiblings(tags)
        for (index, tag) in sortedTags.enumerated() {
            tag.order = index
            tag.version += 1
        }
        
        // 3) Save context
        do {
            try modelContext.save()
        } catch {
            return
        }
    }
    
    // Recursively assign per-sibling-group orders starting from 0 inside each group
    private func assignPerGroupOrders(_ siblings: [YabaCollection]) {
        let sorted = sortSiblings(siblings)
        for (index, item) in sorted.enumerated() {
            item.order = index
            item.version += 1
            // If this item is a folder, recurse into its children (they get their own 0..n-1)
            if item.collectionType == .folder {
                assignPerGroupOrders(item.children)
            }
        }
    }
    
    // Sort siblings based on existing order (or fallback to label)
    private func sortSiblings(_ siblings: [YabaCollection]) -> [YabaCollection] {
        siblings.sorted { a, b in
            if a.order != -1 && b.order != -1 {
                return a.order < b.order
            }
            if a.order != -1 { return true }
            if b.order != -1 { return false }
            return a.label.localizedCompare(b.label) == .orderedAscending
        }
    }

    func onReorderCollection(
        draggedCollectionID: String,
        targetCollectionID: String,
        zone: DropZone
    ) {
        // Skip if dragging onto itself
        if draggedCollectionID == targetCollectionID {
            return
        }

        guard let draggedCollection: YabaCollection = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == draggedCollectionID
                }
            )
        ).first else { return }

        guard let targetCollection: YabaCollection = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == targetCollectionID
                }
            )
        ).first else { return }

        // Prevent moving a parent to its descendant's top/bottom (would break hierarchy)
        if draggedCollection.getDescendants().contains(where: { $0.collectionId == targetCollectionID }) {
            return
        }

        // Tags can only be reordered among tags, not moved into folders
        if draggedCollection.collectionType == .tag || targetCollection.collectionType == .tag {
            // Both must be tags for reordering
            if draggedCollection.collectionType != .tag || targetCollection.collectionType != .tag {
                return
            }

            // Get all tags for reordering
            guard let allCollections: [YabaCollection] = try? modelContext.fetch(
                FetchDescriptor<YabaCollection>()
            ) else { return }

            let allTags = allCollections.filter { $0.collectionType == .tag }
            reorderTags(
                allTags,
                draggedCollection: draggedCollection,
                targetCollection: targetCollection,
                zone: zone
            )
        } else {
            // Both are folders - reorder at the same level
            reorderFolders(
                draggedCollection: draggedCollection,
                targetCollection: targetCollection,
                zone: zone
            )
        }
    }

    private func reorderTags(
        _ allTags: [YabaCollection],
        draggedCollection: YabaCollection,
        targetCollection: YabaCollection,
        zone: DropZone
    ) {
        let sortedTags = sortSiblings(allTags)

        // Create new order by removing dragged collection first
        var newOrder = sortedTags.filter { $0.collectionId != draggedCollection.collectionId }

        // Find target's position in the filtered array
        guard let targetIndexInFiltered = newOrder.firstIndex(where: { $0.collectionId == targetCollection.collectionId }) else {
            return
        }

        // Calculate where to insert the dragged collection
        let insertIndex: Int
        if zone == .top {
            // Insert before target
            insertIndex = targetIndexInFiltered
        } else { // .bottom
            // Insert after target
            insertIndex = targetIndexInFiltered + 1
        }

        // Insert dragged collection at the calculated position
        newOrder.insert(draggedCollection, at: insertIndex)

        // Update orders
        for (index, tag) in newOrder.enumerated() {
            tag.order = index
            tag.version += 1
        }

        do {
            try modelContext.save()
        } catch {
            return
        }
    }

    private func reorderFolders(
        draggedCollection: YabaCollection,
        targetCollection: YabaCollection,
        zone: DropZone
    ) {
        // Determine the target parent
        let targetParent = targetCollection.parent

        // If dragged collection is not already in the target parent, move it there
        let wasMoved = draggedCollection.parent?.collectionId != targetParent?.collectionId
        if wasMoved {
            // Remove from old parent
            if let oldParent = draggedCollection.parent {
                oldParent.children.removeAll(where: { $0.collectionId == draggedCollection.collectionId })

                // Reorder old parent's children
                let sortedOld = sortSiblings(oldParent.children)
                for (index, item) in sortedOld.enumerated() {
                    item.order = index
                    item.version += 1
                }
                oldParent.children = sortedOld
                oldParent.version += 1
            }

            // Add to new parent
            draggedCollection.parent = targetParent
            draggedCollection.version += 1
            if let newParent = targetParent {
                newParent.children.append(draggedCollection)
                newParent.version += 1
            }
        }

        // Get all siblings in the target parent
        let allSiblings: [YabaCollection]
        if let targetParent = targetParent {
            allSiblings = targetParent.children
        } else {
            // Root level - need to update the global collection list
            guard let allCollections: [YabaCollection] = try? modelContext.fetch(
                FetchDescriptor<YabaCollection>()
            ) else { return }
            allSiblings = allCollections.filter { $0.collectionType == .folder && $0.parent == nil }
        }

        // Sort siblings to get current visual order
        let sortedSiblings = sortSiblings(allSiblings)

        // Create new order by removing dragged collection first
        var newOrder = sortedSiblings.filter { $0.collectionId != draggedCollection.collectionId }

        // Find target's position in the filtered array
        guard let targetIndexInFiltered = newOrder.firstIndex(where: { $0.collectionId == targetCollection.collectionId }) else {
            return
        }

        // Calculate where to insert the dragged collection
        let insertIndex: Int
        if zone == .top {
            // Insert before target
            insertIndex = targetIndexInFiltered
        } else { // .bottom
            // Insert after target
            insertIndex = targetIndexInFiltered + 1
        }

        // Insert dragged collection at the calculated position
        newOrder.insert(draggedCollection, at: insertIndex)

        // Update order values and reorder the children array
        for (index, sibling) in newOrder.enumerated() {
            sibling.order = index
            sibling.version += 1
        }

        // Update the parent's children array to match the new order
        if let targetParent = targetParent {
            targetParent.children = newOrder
            targetParent.version += 1
        } else {
            // For root level, we can't directly reorder the fetched array,
            // but the order properties are updated which should be used for sorting
        }

        do {
            try modelContext.save()
        } catch {
            return
        }
    }
}

extension EnvironmentValues {
    @Entry var moveManager: MoveManager = .init()
}
