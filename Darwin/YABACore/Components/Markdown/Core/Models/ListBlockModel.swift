//
//  ListBlockModel.swift
//  YABACore
//

import Foundation

public struct ListBlockModel: Sendable, Equatable {
    public var ordered: Bool
    public var startNumber: Int
    public var bullet: Character
    public var delimiter: Character
    public var tight: Bool
    public var items: [ListItemBlock]
    public var blockAttributes: KMPStringDict
    public init(
        ordered: Bool,
        startNumber: Int,
        bullet: Character,
        delimiter: Character,
        tight: Bool,
        items: [ListItemBlock],
        blockAttributes: KMPStringDict = [:]
    ) {
        self.ordered = ordered
        self.startNumber = startNumber
        self.bullet = bullet
        self.delimiter = delimiter
        self.tight = tight
        self.items = items
        self.blockAttributes = blockAttributes
    }
}

public struct ListItemBlock: Sendable, Equatable {
    public var isTask: Bool
    public var checked: Bool
    public var children: [MarkdownRenderBlock]
    public init(isTask: Bool, checked: Bool, children: [MarkdownRenderBlock]) {
        self.isTask = isTask
        self.checked = checked
        self.children = children
    }
}
