//
//  YabaSchema.swift
//  YABA
//
//  Created by Ali Taha on 28.05.2025.
//

import Foundation
import SwiftData

typealias YabaBookmark = YabaSchemaV1.Bookmark
typealias YabaCollection = YabaSchemaV1.Collection
typealias YabaDataLog = YabaSchemaV1.DataLog

enum YabaSchemaV1: VersionedSchema {
    static var versionIdentifier: Schema.Version = .init(1, 0, 0)
    
    static var models: [any PersistentModel.Type] {
        [Bookmark.self, Collection.self, DataLog.self]
    }
    
    @Model
    final class Bookmark {
        @Attribute(.spotlight)
        var label: String = "" // The only must have field, but thanks to CloudKit, I can't leave it non-optional...
        
        @Attribute(.externalStorage, .allowsCloudEncryption)
        var imageDataHolder: Data? = nil
        
        @Attribute(.externalStorage, .allowsCloudEncryption)
        var iconDataHolder: Data? = nil
        
        var bookmarkId: String = UUID().uuidString
        
        @Attribute(.allowsCloudEncryption)
        var bookmarkDescription: String = ""
        
        @Attribute(.allowsCloudEncryption)
        var link: String = ""
        
        @Attribute(.allowsCloudEncryption)
        var domain: String = ""
        
        var createdAt: Date = Date.now
        var editedAt: Date = Date.now
        
        @Attribute(.allowsCloudEncryption)
        var imageUrl: String? = nil
        
        @Attribute(.allowsCloudEncryption)
        var iconUrl: String? = nil
        
        @Attribute(.allowsCloudEncryption)
        var videoUrl: String? = nil
        
        @Attribute(.allowsCloudEncryption)
        var readableHTML: String? = nil
        
        var type: Int = 1
        var version: Int = 1
        var collections: [YabaCollection]? = []
        
        var bookmarkType: BookmarkType {
            BookmarkType(rawValue: type) ?? .none
        }

        init(
            bookmarkId: String = UUID().uuidString,
            link: String = "",
            label: String = "",
            bookmarkDescription: String = "",
            domain: String = "",
            createdAt: Date = .now,
            editedAt: Date = .now,
            imageDataHolder: Data?,
            iconDataHolder: Data?,
            imageUrl: String?,
            iconUrl: String?,
            videoUrl: String?,
            readableHTML: String?,
            type: BookmarkType = .none,
            version: Int = 1,
            collections: [YabaCollection]? = [],
        ) {
            self.bookmarkId = bookmarkId
            self.link = link
            self.label = label
            self.bookmarkDescription = bookmarkDescription
            self.domain = domain
            self.createdAt = createdAt
            self.editedAt = editedAt
            self.imageDataHolder = imageDataHolder
            self.iconDataHolder = iconDataHolder
            self.imageUrl = imageUrl
            self.iconUrl = iconUrl
            self.videoUrl = videoUrl
            self.readableHTML = readableHTML
            self.type = type.rawValue
            self.version = version
            self.collections = collections
        }
    }
    
    @Model
    final class Collection {
        // Below relationship already covers this one.
        var parent: Collection? = nil
                
        @Relationship(inverse: \Collection.parent)
        var children: [Collection] = []
        
        @Relationship(inverse: \Bookmark.collections)
        var bookmarks: [Bookmark]? = []

        var collectionId: String = UUID().uuidString
        var label: String = ""
        var icon: String = "folder-01"
        var createdAt: Date = Date.now
        var editedAt: Date = Date.now
        var color: YabaColor = YabaColor.none
        var order: Int = -1 // Used only in custom ordering active
        var type: Int = 1
        var version: Int = 1
        
        var collectionType: CollectionType {
            CollectionType(rawValue: type) ?? .folder
        }

        init(
            collectionId: String = UUID().uuidString,
            label: String = "",
            icon: String = "folder-01",
            createdAt: Date = .now,
            editedAt: Date = .now,
            bookmarks: [Bookmark]? = [],
            color: YabaColor = .none,
            type: CollectionType = .folder,
            version: Int = 1,
            parent: YabaCollection? = nil,
            children: [YabaCollection] = [],
            order: Int = -1,
        ) {
            self.collectionId = collectionId
            self.label = label
            self.createdAt = createdAt
            self.editedAt = editedAt
            self.bookmarks = bookmarks
            self.icon = icon
            self.color = color
            self.type = type.rawValue
            self.version = version
            self.parent = parent
            self.children = children
            self.order = order
        }
    }
    
    /**
     - Initially designed to hold the all data.
     - Now, it is just a tombstone of all delete operations.
     */
    @Model
    final class DataLog {
        var logId: String = UUID().uuidString
        var entityId: String = ""
        var entityType: EntityType = EntityType.bookmark
        var actionType: ActionType = ActionType.deleted
        var timestamp: Date = Date.now

        var fieldChangesJSON: String?
        var fieldChanges: [FieldChange]? {
            guard let fieldChangesJSON,
                  let data = fieldChangesJSON.data(using: .utf8)
            else { return nil }

            return try? JSONDecoder().decode([FieldChange].self, from: data)
        }

        init(
            logId: String = UUID().uuidString,
            entityId: String = "",
            entityType: EntityType = .bookmark,
            actionType: ActionType = .deleted,
            timestamp: Date = .now,
            fieldChanges: [FieldChange]? = nil
        ) {
            self.logId = logId
            self.entityId = entityId
            self.entityType = entityType
            self.actionType = actionType
            self.timestamp = timestamp
            self.fieldChangesJSON = fieldChanges.flatMap {
                try? String(data: JSONEncoder().encode($0), encoding: .utf8)
            }
        }
    }
}
