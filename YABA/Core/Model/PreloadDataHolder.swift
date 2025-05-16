//
// PreloadDataHolder.swift
// YABA
//
// Created by Ali Taha on 14.10.2024.
//

import Foundation

struct PreloadDataHolder: Codable {
    let collections: [PreloadCollection]

    func getFolderModels() -> [YabaCollection] {
        return self.collections.map { $0.toCollectionModel() }
    }
}

struct PreloadCollection: Codable {
    let label: String
    let icon: String
    let color: YabaColor

    func toCollectionModel() -> YabaCollection {
        return YabaCollection(
            collectionId: UUID().uuidString,
            label: self.label,
            icon: self.icon,
            createdAt: .now,
            editedAt: .now,
            bookmarks: [],
            color: self.color,
            type: .folder
        )
    }

    private enum CodingKeys: String, CodingKey {
        case label = "label"
        case icon = "icon"
        case color = "color"
    }
}
