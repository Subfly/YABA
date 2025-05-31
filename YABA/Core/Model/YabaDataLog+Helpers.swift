//
//  YabaDataLog+Helpers.swift
//  YABA
//
//  Created by Ali Taha on 28.05.2025.
//

import Foundation

enum EntityType: String, Codable {
    case bookmark, collection
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

struct FieldChange: Codable {
    let key: Field
    let newValue: String?
}

class YabaDataLogUtil {
    static func generateFieldChanges(
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

    private static func areEqual(_ lhs: Any?, _ rhs: Any?) -> Bool {
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

    private static func stringify(_ value: Any?) -> String? {
        switch value {
        case let v as String: return v
        case let v as Int: return String(v)
        case let v as Bool: return String(v)
        case nil: return ""
        default: return "\(value ?? "")"
        }
    }
}
