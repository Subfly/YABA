//
//  MarkdownKMPTableSupport.swift
//  YABACore
//
//  Maps a KMP `Table` to `TableBlock` for the shared table grid view (not a second document AST).
//

import Foundation
import MarkdownParser

enum MarkdownKMPTableSupport {
    static func tableBlock(from table: Table) -> TableBlock {
        var align: [TableBlock.ColumnAlignment] = table.columnAlignments.map(mapColumnAlignment)
        var header: [InlineContent]?
        var body: [[InlineContent]] = []
        if let head = table.children.first(where: { $0 is TableHead }) as? TableHead {
            if let row = head.children.compactMap({ $0 as? TableRow }).first {
                let cells = tableRowCells(row)
                if !cells.isEmpty { header = cells }
            }
        }
        if let tBody = table.children.first(where: { $0 is TableBody }) as? TableBody {
            for child in tBody.children {
                guard let row = child as? TableRow else { continue }
                let cells = tableRowCells(row)
                if !cells.isEmpty { body.append(cells) }
            }
        }
        let colCount = max(header?.count ?? 0, body.map(\.count).max() ?? 0, align.count)
        if colCount > 0, align.count < colCount {
            align.append(contentsOf: Array(repeating: .none, count: colCount - align.count))
        }
        return TableBlock(columnAlignment: align, headerRow: header, bodyRows: body)
    }

    private static func tableRowCells(_ row: TableRow) -> [InlineContent] {
        var cells: [InlineContent] = []
        for cell in row.children {
            if let tc = cell as? TableCell {
                cells.append(InlineAssembler.build(from: tc))
            }
        }
        return cells
    }

    private static func mapColumnAlignment(_ a: Table.Alignment) -> TableBlock.ColumnAlignment {
        if a === Table.Alignment.left { return .left }
        if a === Table.Alignment.right { return .right }
        if a === Table.Alignment.center { return .center }
        if a === Table.Alignment.none { return .none }
        return .none
    }
}
