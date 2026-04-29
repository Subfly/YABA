//
//  MarkdownTableBlockView.swift
//  YABACore
//
//  GFM tables: `Grid` inside a horizontal `ScrollView` so every column is visible on compact width.
//  (SwiftUI `Table` hides all but the first column in `horizontalSizeClass == .compact`.)
//  Vertical scrolling stays with the parent markdown stack; only horizontal overflow scrolls here.
//

import SwiftUI

// MARK: - Table (GFM)

struct MarkdownTableBlockView: View {
    let table: TableBlock
    let theme: MarkdownThemeTokens

    var body: some View {
        let layout = MarkdownTableLayout(table: table)
        if layout.columnCount == 0 {
            EmptyView()
        } else {
            ScrollView(.horizontal, showsIndicators: true) {
                Grid(horizontalSpacing: 0, verticalSpacing: 0) {
                    GridRow {
                        ForEach(0..<layout.columnCount, id: \.self) { columnIndex in
                            MarkdownSelectablePlainText(
                                verbatim: layout.columnHeaderTitle(columnIndex),
                                semantic: .subheadline,
                                weight: .semibold,
                                monospaced: false
                            )
                                .multilineTextAlignment(headerTextAlignment(layout.alignment(at: columnIndex)))
                                .frame(maxWidth: .infinity, alignment: cellFrameAlignment(layout.alignment(at: columnIndex)))
                                .padding(6)
                                .border(theme.tableBorder, width: 0.5)
                        }
                    }
                    ForEach(layout.bodyRows) { row in
                        GridRow {
                            ForEach(0..<layout.columnCount, id: \.self) { columnIndex in
                                cellView(row: row, columnIndex: columnIndex, layout: layout)
                                    .border(theme.tableBorder, width: 0.5)
                            }
                        }
                    }
                }
                .fixedSize(horizontal: true, vertical: true)
            }
            .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
        }
    }

    @ViewBuilder
    private func cellView(
        row: MarkdownTableDisplayRow,
        columnIndex: Int,
        layout: MarkdownTableLayout
    ) -> some View {
        let cell = row.cell(at: columnIndex)
        let align = layout.alignment(at: columnIndex)
        MarkdownInlineBlockView(content: cell, typography: .tableCell)
            .multilineTextAlignment(headerTextAlignment(align))
            .padding(6)
            .frame(maxWidth: .infinity, alignment: cellFrameAlignment(align))
    }

    private func headerTextAlignment(_ a: TableBlock.ColumnAlignment) -> TextAlignment {
        switch a {
        case .left, .none: return .leading
        case .center: return .center
        case .right: return .trailing
        }
    }

    /// `frame(alignment:)` takes `Alignment`, not `HorizontalAlignment`.
    private func cellFrameAlignment(_ a: TableBlock.ColumnAlignment) -> Alignment {
        switch a {
        case .left: return .leading
        case .center: return .center
        case .right: return .trailing
        case .none: return .leading
        }
    }
}

// MARK: - Layout

/// Normalized model for table rows: one identifiable row per GFM body line.
private struct MarkdownTableDisplayRow: Identifiable {
    let id: Int
    var cells: [InlineContent]

    func cell(at index: Int) -> InlineContent {
        guard index >= 0, index < cells.count else { return .empty }
        return cells[index]
    }
}

private struct MarkdownTableLayout {
    let table: TableBlock
    let columnCount: Int
    let bodyRows: [MarkdownTableDisplayRow]
    private let _columnAlignment: [TableBlock.ColumnAlignment]

    init(table: TableBlock) {
        self.table = table
        _columnAlignment = table.columnAlignment
        let header = table.headerRow
        let headerCount = header?.count ?? 0
        let alignCount = table.columnAlignment.count
        let bodyMax = table.bodyRows.map(\.count).max() ?? 0
        // Delimiter row (`| --- |`) defines the canonical column count; rows may pad/trim to match.
        let n = max(alignCount, headerCount, bodyMax)
        self.columnCount = n
        guard n > 0 else {
            self.bodyRows = []
            return
        }
        self.bodyRows = table.bodyRows.enumerated().map { index, line in
            MarkdownTableDisplayRow(
                id: index,
                cells: Self.normalizeRow(line, columnCount: n)
            )
        }
    }

    func columnHeaderTitle(_ index: Int) -> String {
        if let header = table.headerRow, index < header.count {
            let t = MarkdownInlinePlainText.plainText(from: header[index])
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if !t.isEmpty { return t }
        }
        return "Column \(index + 1)"
    }

    func alignment(at columnIndex: Int) -> TableBlock.ColumnAlignment {
        guard columnIndex < _columnAlignment.count else { return .none }
        return _columnAlignment[columnIndex]
    }

    private static func normalizeRow(_ row: [InlineContent], columnCount: Int) -> [InlineContent] {
        if row.count == columnCount { return row }
        if row.count > columnCount { return Array(row.prefix(columnCount)) }
        return row + Array(repeating: .empty, count: columnCount - row.count)
    }
}
