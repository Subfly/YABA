//
//  Collection.swift
//  YABA
//
//  Created by Ali Taha on 8.10.2024.
//

import Foundation
import SwiftData

@Model
final class YabaCollection {
    @Relationship(inverse: \Bookmark.collections)
    var bookmarks: [Bookmark] = []

    var collectionId: String
    var label: String
    var icon: String
    var createdAt: Date
    var editedAt: Date
    var color: YabaColor
    var type: Int
    
    var collectionType: CollectionType {
        CollectionType(rawValue: type) ?? .folder
    }

    init(
        collectionId: String,
        label: String,
        icon: String,
        createdAt: Date,
        editedAt: Date,
        bookmarks: [Bookmark] = [],
        color: YabaColor,
        type: CollectionType
    ) {
        self.collectionId = collectionId
        self.label = label
        self.createdAt = createdAt
        self.editedAt = editedAt
        self.bookmarks = bookmarks
        self.icon = icon
        self.color = color
        self.type = type.rawValue
    }
    
    static func empty() -> YabaCollection {
        return YabaCollection(
            collectionId: UUID().uuidString,
            label: "",
            icon: "",
            createdAt: .now,
            editedAt: .now,
            bookmarks: [],
            color: .none,
            type: .folder
        )
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
