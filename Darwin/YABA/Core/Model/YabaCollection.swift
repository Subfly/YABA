//
//  Collection.swift
//  YABA
//
//  Created by Ali Taha on 8.10.2024.
//

import Foundation
import SwiftUI
import SwiftData

/// FOR THE MODEL ITSELF, LOOK TO _YABA_SCHEMA_
extension YabaCollection {
    static func empty() -> YabaCollection {
        return YabaCollection(
            collectionId: UUID().uuidString,
            label: "",
            icon: "",
            createdAt: .now,
            editedAt: .now,
            bookmarks: [],
            color: .none,
            type: .folder,
            version: 0,
        )
    }
    
    func getParentColorsInOrder() -> [YabaColor] {
        var colors: [YabaColor] = []
        var current: YabaCollection? = self
        
        while let parent = current?.parent {
            colors.append(parent.color)
            current = current?.parent
        }
        
        return colors.reversed()
    }
    
    func getDescendants() -> [YabaCollection] {
        var result: [YabaCollection] = []
        var stack: [YabaCollection] = children
        while let current = stack.popLast() {
            result.append(current)
            stack.append(contentsOf: current.children)
        }
        return result
    }
    
    func hasChanges(with other: YabaCollection) -> Bool {
        let idHasChanges = self.id != other.id
        let collectionIdHasChanges = self.collectionId != other.collectionId
        let labelHasChanges = self.label.localizedStandardCompare(other.label) != .orderedSame
        let iconHasChanges = self.icon.localizedStandardCompare(other.icon) != .orderedSame
        let colorHasChanges = self.color != other.color
        return idHasChanges || collectionIdHasChanges || labelHasChanges || iconHasChanges || colorHasChanges
    }
}
