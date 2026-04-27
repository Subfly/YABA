//
//  TableBlock.swift
//  YABACore
//

import Foundation

public struct TableBlock: Sendable, Equatable {
    public var columnAlignment: [ColumnAlignment]
    /// First row from `TableHead`, if present.
    public var headerRow: [InlineContent]?
    /// Rows from `TableBody` (or non-head rows if head empty).
    public var bodyRows: [[InlineContent]]
    public init(columnAlignment: [ColumnAlignment], headerRow: [InlineContent]?, bodyRows: [[InlineContent]]) {
        self.columnAlignment = columnAlignment
        self.headerRow = headerRow
        self.bodyRows = bodyRows
    }

    public enum ColumnAlignment: Sendable, Equatable {
        case left, center, right, none
    }
}
