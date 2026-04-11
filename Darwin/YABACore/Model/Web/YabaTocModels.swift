//
//  YabaTocModels.swift
//  YABACore
//

import Foundation

public struct YabaToc: Sendable, Codable, Equatable {
    public var items: [YabaTocItem]

    public init(items: [YabaTocItem] = []) {
        self.items = items
    }
}

public struct YabaTocItem: Sendable, Codable, Equatable {
    public var id: String
    public var title: String
    public var level: Int
    public var children: [YabaTocItem]
    public var extrasJson: String?

    public init(
        id: String,
        title: String,
        level: Int,
        children: [YabaTocItem] = [],
        extrasJson: String? = nil
    ) {
        self.id = id
        self.title = title
        self.level = level
        self.children = children
        self.extrasJson = extrasJson
    }
}
