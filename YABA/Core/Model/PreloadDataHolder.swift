//
// PreloadDataHolder.swift
// YABA
//
// Created by Ali Taha on 14.10.2024.
//

import Foundation

struct PreloadDataHolder: Codable {
    let folders: [PreloadCollection]
    let tags: [PreloadCollection]

    func allCollections() -> [YabaCollection] {
        let folderModels = folders.map { $0.toCollectionModel(type: .folder) }
        let tagModels = tags.map { $0.toCollectionModel(type: .tag) }
        return folderModels + tagModels
    }
}

struct PreloadCollection: Codable {
    let label: String
    let icon: String
    let color: Int

    func toCollectionModel(type: CollectionType) -> YabaCollection {
        let now = Date()
        return YabaCollection(
            collectionId: UUID().uuidString,
            label: label,
            icon: icon,
            createdAt: now,
            editedAt: now,
            bookmarks: [],
            color: YabaColor(rawValue: color) ?? .none,
            type: type,
            version: 0
        )
    }
}
