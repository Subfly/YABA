//
//  YabaDataLog+Helpers.swift
//  YABA
//
//  Created by Ali Taha on 28.05.2025.
//

import Foundation
import SwiftData

enum LoggerError: Error {
    case modelContextNotGiven
}

enum EntityType: String, Codable {
    case bookmark, collection, all
}

enum ActionType: String, Codable {
    case created, updated, deleted, deletedAll
}

enum Field: String, Codable {
    case id, label, description,
         link, domain, createdAt,
         editedAt, image, icon,
         video, type, color, collections
}

struct FieldChange: Codable, Hashable {
    let key: Field
    let newValue: String?
}

@MainActor
class YabaDataLogger {
    private var modelContext: ModelContext?
    static let shared: YabaDataLogger = .init()
    
    private init() {}
    
    func setContext(_ modelContext: ModelContext) {
        self.modelContext = modelContext
    }
    
    func logCollectionChange(
        old: YabaCollection?,
        new: YabaCollection,
        shouldSave: Bool,
    ) throws {
        guard let modelContext else { throw LoggerError.modelContextNotGiven }
        
        if let old {
            let changes = generateFieldChanges(
                old: [
                    .label: old.label,
                    .icon: old.icon,
                    .color: old.color.rawValue
                ],
                new: [
                    .label: new.label,
                    .icon: new.icon,
                    .color: new.color.rawValue
                ]
            )
            
            if !changes.isEmpty {
                let entry = YabaDataLog(
                    entityId: old.collectionId,
                    entityType: .collection,
                    actionType: .updated,
                    fieldChanges: changes,
                )
                modelContext.insert(entry)
            }
        } else {
            let changes = generateFieldChanges(
                old: [:],
                new: [
                    .label: new.label,
                    .icon: new.icon,
                    .color: new.color.rawValue
                ]
            )
            
            if !changes.isEmpty {
                let entry = YabaDataLog(
                    entityId: new.collectionId,
                    entityType: .collection,
                    actionType: .created,
                    fieldChanges: changes,
                )
                modelContext.insert(entry)
            }
        }
        
        if shouldSave {
            try? modelContext.save()
        }
    }
    
    func logBookmarkChange(
        old: YabaBookmark?,
        new: YabaBookmark,
        shouldSave: Bool,
    ) throws {
        guard let modelContext else { throw LoggerError.modelContextNotGiven }
        
        if let old {
            var changes = generateFieldChanges(
                old: [
                    .label: old.label,
                    .link: old.link,
                    .domain: old.domain,
                    .description: old.bookmarkDescription,
                    .image: old.imageUrl,
                    .icon: old.iconUrl,
                    .video: old.videoUrl,
                    .type: old.type
                ],
                new: [
                    .label: new.label,
                    .link: new.link,
                    .domain: new.domain,
                    .description: new.bookmarkDescription,
                    .image: new.imageUrl,
                    .icon: new.iconUrl,
                    .video: new.videoUrl,
                    .type: new.type
                ]
            )
            
            let collectionIds = new.collections.map { $0.collectionId }
            if let data = try? JSONEncoder().encode(collectionIds),
               let json = String(data: data, encoding: .utf8){
                changes.append(.init(key: .collections, newValue: json))
            }
            
            if !changes.isEmpty {
                let entry = YabaDataLog(
                    entityId: old.bookmarkId,
                    entityType: .bookmark,
                    actionType: .updated,
                    fieldChanges: changes,
                )
                modelContext.insert(entry)
            }
        } else {
            var changes = generateFieldChanges(
                old: [:],
                new: [
                    .label: new.label,
                    .link: new.link,
                    .domain: new.domain,
                    .description: new.bookmarkDescription,
                    .image: new.imageUrl,
                    .icon: new.iconUrl,
                    .video: new.videoUrl,
                    .type: new.type
                ]
            )
            
            let collectionIds = new.collections.map { $0.collectionId }
            if let data = try? JSONEncoder().encode(collectionIds),
               let json = String(data: data, encoding: .utf8){
                changes.append(.init(key: .collections, newValue: json))
            }
            
            if !changes.isEmpty {
                let entry = YabaDataLog(
                    entityId: new.bookmarkId,
                    entityType: .bookmark,
                    actionType: .created,
                    fieldChanges: changes,
                )
                modelContext.insert(entry)
            }
        }
        
        if shouldSave {
            try? modelContext.save()
        }
    }
    
    func logCollectionDelete(
        id: String,
        shouldSave: Bool,
    ) throws {
        guard let modelContext else { throw LoggerError.modelContextNotGiven }
        let entry = YabaDataLog(
            entityId: id,
            entityType: .collection,
            actionType: .deleted,
            fieldChanges: nil,
        )
        modelContext.insert(entry)
        
        if shouldSave {
            try? modelContext.save()
        }
    }
    
    func logBookmarkDelete(
        id: String,
        shouldSave: Bool
    ) throws {
        guard let modelContext else { throw LoggerError.modelContextNotGiven }
        let entry = YabaDataLog(
            entityId: id,
            entityType: .bookmark,
            actionType: .deleted,
            fieldChanges: nil,
        )
        modelContext.insert(entry)
        
        if shouldSave {
            try? modelContext.save()
        }
    }
    
    func logBulkDelete(
        shouldSave: Bool,
    ) throws {
        guard let modelContext else { throw LoggerError.modelContextNotGiven }
        
        // The previous state of anything is not important anymore
        try? modelContext.delete(model: YabaDataLog.self)
        
        // Save the timestamp as to determine when this operation done
        let event = YabaDataLog(
            entityId: UUID().uuidString,
            entityType: .all,
            actionType: .deletedAll
        )
        modelContext.insert(event)
        
        if shouldSave {
            try? modelContext.save()
        }
    }
    
    private func generateFieldChanges(
        old: [Field: Any?],
        new: [Field: Any?]
    ) -> [FieldChange] {
        var changes: [FieldChange] = []
        
        // New
        if old.isEmpty {
            for key in new.keys {
                let newVal = new[key] ?? nil
                let stringValue = stringify(newVal)
                changes.append(FieldChange(key: key, newValue: stringValue))
            }
            return changes
        }

        // Edit
        for key in Set(old.keys).union(new.keys) {
            let oldVal = old[key] ?? nil
            let newVal = new[key] ?? nil

            if !areEqual(oldVal, newVal) {
                let stringValue = stringify(newVal)
                changes.append(FieldChange(key: key, newValue: stringValue))
            }
        }

        return changes
    }

    private func areEqual(_ lhs: Any?, _ rhs: Any?) -> Bool {
        switch (lhs, rhs) {
        case (nil, nil):
            return true
        case let (l as String, r as String):
            return l == r
        case let (l as Int, r as Int):
            return l == r
        case let (l as Bool, r as Bool):
            return l == r
        default:
            return false
        }
    }

    private func stringify(_ value: Any?) -> String? {
        switch value {
        case let v as String: return v
        case let v as Int: return String(v)
        case let v as Bool: return String(v)
        case nil: return ""
        default: return "\(value ?? "")"
        }
    }
}
