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
        let bookmark: YabaBookmark? = try? modelContext.fetch(
            FetchDescriptor<YabaBookmark>(
                predicate: #Predicate {
                    $0.bookmarkId == bookmarkID
                }
            )
        ).first
        
        guard let bookmark else {
            return
        }
        
        let collection: YabaCollection? = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == toCollectionID
                }
            )
        ).first
        
        guard let collection else {
            return
        }
        
        // MARK TAG INTERACTIONS
        if collection.collectionType == .tag {
            if collection.bookmarks?.contains(bookmark) == true {
                return
            }
            
            // Add tag to bookmark
            do {
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
            let oldParent = bookmark.collections?.first { innerCollection in
                innerCollection.collectionType == .folder
            }
            
            guard let oldParent else {
                return
            }
            
            oldParent.bookmarks?.removeAll { innerBookmark in
                innerBookmark.bookmarkId == bookmark.bookmarkId
            }
            
            // MOVE BOOKMARK TO NEW PARENT
            collection.bookmarks?.append(bookmark)
            
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
        
        let folder1: YabaCollection? = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == folder1ID
                }
            )
        ).first
        
        guard let folder1 else {
            return
        }
        
        let folder2: YabaCollection? = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == folder2ID
                }
            )
        ).first
        
        guard let folder2 else {
            return
        }
        
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
                }
                oldParent.children = sortedOld
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
            }
            folder2.children = sortedNew
            
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
}

extension EnvironmentValues {
    @Entry var moveManager: MoveManager = .init()
}
