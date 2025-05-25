//
//  ModelMapper.swift
//  YABA
//
//  Created by Ali Taha on 23.05.2025.
//

import Foundation

extension YabaCollection {
    func mapToCodable() -> YabaCodableCollection {
        return .init(
            collectionId: self.collectionId,
            label: self.label,
            icon: self.icon,
            createdAt: self.createdAt.ISO8601Format(),
            editedAt: self.editedAt.ISO8601Format(),
            color: self.color.rawValue,
            type: self.type,
            bookmarks: self.bookmarks.map { bookmark in
                bookmark.bookmarkId
            }
        )
    }
}

extension YabaCodableCollection {
    func mapToModel() -> YabaCollection {
        let color = YabaColor(rawValue: self.color) ?? .none
        let type = CollectionType(rawValue: self.type) ?? .folder
        
        return .init(
            collectionId: self.collectionId,
            label: self.label,
            icon: self.icon,
            createdAt: ISO8601DateFormatter().date(from: self.createdAt) ?? .now,
            editedAt: ISO8601DateFormatter().date(from: self.editedAt) ?? .now,
            bookmarks: [], // Set as empty as it will be filled in another iteration.
            color: color,
            type: type
        )
    }
}

extension Bookmark {
    func mapToCodable() -> YabaCodableBookmark {
        return .init(
            bookmarkId: self.bookmarkId,
            label: self.label,
            bookmarkDescription: self.bookmarkDescription,
            link: self.link,
            domain: self.domain,
            createdAt: self.createdAt.ISO8601Format(),
            editedAt: self.editedAt.ISO8601Format(),
            imageUrl: self.imageUrl,
            iconUrl: self.iconUrl,
            videoUrl: self.videoUrl,
            type: self.type
        )
    }
}

extension YabaCodableBookmark {
    func mapToModel() -> Bookmark {
        let type = BookmarkType(rawValue: self.type ?? 1) ?? .none
        let currentDate = Date.now.ISO8601Format()
        
        return .init(
            bookmarkId: self.bookmarkId ?? UUID().uuidString,
            link: self.link,
            label: self.label ?? self.link,
            bookmarkDescription: self.bookmarkDescription ?? "",
            domain: self.domain ?? "",
            createdAt: ISO8601DateFormatter().date(from: self.createdAt ?? currentDate) ?? .now,
            editedAt: ISO8601DateFormatter().date(from: self.editedAt ?? currentDate) ?? .now,
            imageData: nil,
            iconData: nil,
            imageUrl: self.imageUrl,
            iconUrl: self.iconUrl,
            videoUrl: self.videoUrl,
            type: type,
            collections: [] // This will be handled by another iteration
        )
    }
}
