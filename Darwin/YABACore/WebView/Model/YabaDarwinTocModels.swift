//
//  YabaDarwinTocModels.swift
//  YABACore
//

import Foundation

public struct YabaDarwinToc: Sendable, Codable, Equatable {
    public var items: [YabaDarwinTocItem]

    public init(items: [YabaDarwinTocItem] = []) {
        self.items = items
    }
}

public struct YabaDarwinTocItem: Sendable, Codable, Equatable {
    public var id: String
    public var title: String
    public var level: Int
    public var children: [YabaDarwinTocItem]
    public var extrasJson: String?

    public init(
        id: String,
        title: String,
        level: Int,
        children: [YabaDarwinTocItem] = [],
        extrasJson: String? = nil
    ) {
        self.id = id
        self.title = title
        self.level = level
        self.children = children
        self.extrasJson = extrasJson
    }
}
