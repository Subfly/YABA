//
//  YabaLegacySchemaSupportTypes.swift
//  YABACore
//
//  V1 migration-only support types to keep legacy schema self-contained in YABACore.
//

import Foundation

enum YabaLegacyBookmarkType: Int, Codable, CaseIterable {
    case none = 1
    case webLink = 2
    case video = 3
    case image = 4
    case audio = 5
    case music = 6
}

enum YabaLegacyCollectionType: Int, Codable, CaseIterable {
    case folder = 1
    case tag = 2
}

enum YabaLegacyColor: Int, Codable, CaseIterable {
    case none = 0
    case blue = 1
    case brown = 2
    case cyan = 3
    case gray = 4
    case green = 5
    case indigo = 6
    case mint = 7
    case orange = 8
    case pink = 9
    case purple = 10
    case red = 11
    case teal = 12
    case yellow = 13
}

enum YabaLegacyEntityType: String, Codable {
    case bookmark
    case collection
    case all
}

enum YabaLegacyActionType: String, Codable {
    case created
    case updated
    case deleted
    case deletedAll
}

enum YabaLegacyField: String, Codable {
    case id
    case label
    case description
    case link
    case domain
    case createdAt
    case editedAt
    case image
    case icon
    case video
    case type
    case color
    case collections
}

struct YabaLegacyFieldChange: Codable, Hashable {
    let key: YabaLegacyField
    let newValue: String?
}
