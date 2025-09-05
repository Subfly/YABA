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
            bookmarks: self.bookmarks?.map { bookmark in
                bookmark.bookmarkId
            } ?? [],
            version: self.version
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
            type: type,
            version: self.version
        )
    }
}

extension YabaBookmark {
    /// Maps to codable format for exports (JSON, CSV, HTML) - excludes image data
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
            readableHTML: nil, // Exclude HTML content to prevent import/export issues
            type: self.type,
            version: self.version,
            imageData: nil, // Explicitly exclude image data for exports
            iconData: nil   // Explicitly exclude icon data for exports
        )
    }
    
    /// Maps to codable format for sync - includes image data
    func mapToCodableForSync() -> YabaCodableBookmark {
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
            readableHTML: nil, // Exclude HTML content to prevent sync issues
            type: self.type,
            version: self.version,
            imageData: self.imageDataHolder, // Include image data for sync
            iconData: self.iconDataHolder    // Include icon data for sync
        )
    }
}

extension YabaCodableBookmark {
    func mapToModel() -> YabaBookmark {
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
            imageDataHolder: self.imageData, // Include image data from sync
            iconDataHolder: self.iconData,   // Include icon data from sync
            imageUrl: self.imageUrl,
            iconUrl: self.iconUrl,
            videoUrl: self.videoUrl,
            readableHTML: nil, // Always set to nil to prevent import/export issues
            type: type,
            version: self.version ?? 0,
            collections: [] // This will be handled by another iteration
        )
    }
}
