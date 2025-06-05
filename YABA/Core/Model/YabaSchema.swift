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
        var label: String
        
        var bookmarkId: String
        var bookmarkDescription: String
        var link: String
        var domain: String
        var createdAt: Date
        var editedAt: Date
        var imageUrl: String?
        var imageDataHolder: DataHolder?
        var iconUrl: String?
        var iconDataHolder: DataHolder?
        var videoUrl: String?
        var readableHTML: String?
        var type: Int
        var collections: [YabaCollection]
        
        var bookmarkType: BookmarkType {
            BookmarkType(rawValue: type) ?? .none
        }

        init(
            bookmarkId: String,
            link: String,
            label: String,
            bookmarkDescription: String,
            domain: String,
            createdAt: Date,
            editedAt: Date,
            imageDataHolder: DataHolder?,
            iconDataHolder: DataHolder?,
            imageUrl: String?,
            iconUrl: String?,
            videoUrl: String?,
            readableHTML: String?,
            type: BookmarkType,
            collections: [YabaCollection] = []
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
            self.collections = collections
        }
    }
    
    @Model
    final class Collection {
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
    }
    
    @Model
    final class DataLog {
        var logId: String
        var entityId: String
        var entityType: EntityType
        var actionType: ActionType
        var timestamp: Date

        var fieldChangesJSON: String?
        var fieldChanges: [FieldChange]? {
            guard let fieldChangesJSON,
                  let data = fieldChangesJSON.data(using: .utf8)
            else { return nil }

            return try? JSONDecoder().decode([FieldChange].self, from: data)
        }

        init(
            logId: String = UUID().uuidString,
            entityId: String,
            entityType: EntityType,
            actionType: ActionType,
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
    
    @Model
    final class DataHolder {
        @Attribute(.externalStorage, .allowsCloudEncryption)
        var data: Data?
        
        init(data: Data?) {
            self.data = data
        }
    }
}
