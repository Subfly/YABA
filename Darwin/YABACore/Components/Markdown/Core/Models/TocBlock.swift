//
//  TocBlock.swift
//  YABACore
//

import Foundation

public struct TocBlock: Sendable, Equatable {
    public var minDepth: Int
    public var maxDepth: Int
    public var excludeIds: [String]
    public var order: String
    /// Pre-resolved entries from the document (optional; may be empty if not computed).
    public var resolvedEntries: [TocEntry]
    public init(
        minDepth: Int,
        maxDepth: Int,
        excludeIds: [String],
        order: String,
        resolvedEntries: [TocEntry] = []
    ) {
        self.minDepth = minDepth
        self.maxDepth = maxDepth
        self.excludeIds = excludeIds
        self.order = order
        self.resolvedEntries = resolvedEntries
    }
}

public struct TocEntry: Sendable, Equatable, Identifiable {
    public var id: String
    public var level: Int
    public var title: String
    public var headingId: String?
    public init(level: Int, title: String, headingId: String?) {
        self.level = level
        self.title = title
        self.headingId = headingId
        self.id = headingId ?? "\(level)-\(title.hashValue)"
    }
}
