//
//  TocModels.swift
//  YABACore
//

import Foundation

public struct Toc: Sendable, Codable, Equatable {
    public var items: [TocItem]

    public init(items: [TocItem] = []) {
        self.items = items
    }
}

public struct TocItem: Sendable, Codable, Equatable {
    public var id: String
    public var title: String
    public var level: Int
    public var children: [TocItem]
    public var extrasJson: String?

    public init(
        id: String,
        title: String,
        level: Int,
        children: [TocItem] = [],
        extrasJson: String? = nil
    ) {
        self.id = id
        self.title = title
        self.level = level
        self.children = children
        self.extrasJson = extrasJson
    }
}
