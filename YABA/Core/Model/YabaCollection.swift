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

    var label: String
    var icon: String
    var createdAt: Date
    var color: YabaColor
    var type: Int
    
    var collectionType: CollectionType {
        CollectionType(rawValue: type) ?? .folder
    }

    init(
        label: String,
        icon: String,
        createdAt: Date,
        bookmarks: [Bookmark] = [],
        color: YabaColor,
        type: CollectionType
    ) {
        self.label = label
        self.createdAt = createdAt
        self.bookmarks = bookmarks
        self.icon = icon
        self.color = color
        self.type = type.rawValue
    }
    
    static func empty() -> YabaCollection {
        return YabaCollection(
            label: "",
            icon: "",
            createdAt: .now,
            bookmarks: [],
            color: .none,
            type: .folder
        )
    }
    
    func hasChanges(with other: YabaCollection) -> Bool {
        let idHasChanges = self.id != other.id
        let labelHasChanges = self.label.localizedStandardCompare(other.label) != .orderedSame
        let iconHasChanges = self.icon.localizedStandardCompare(other.icon) != .orderedSame
        let colorHasChanges = self.color != other.color
        return idHasChanges || labelHasChanges || iconHasChanges || colorHasChanges
    }
}
