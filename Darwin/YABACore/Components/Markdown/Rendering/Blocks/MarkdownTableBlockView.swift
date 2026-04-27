//
//  MarkdownTableBlockView.swift
//  YABACore
//
//  Render-model table (grid) layout.
//

import SwiftUI

// MARK: - Table (grid)

struct MarkdownTableBlockView: View {
    let table: TableBlock
    let theme: MarkdownThemeTokens

    var body: some View {
        ScrollView([.vertical, .horizontal], showsIndicators: true) {
            VStack(alignment: .leading, spacing: 0) {
                if let header = table.headerRow {
                    rowView(cells: header, isHeader: true)
                    Divider()
                }
                ForEach(Array(table.bodyRows.enumerated()), id: \.offset) { i, r in
                    rowView(cells: r, isHeader: false)
                    if i < table.bodyRows.count - 1 { Divider() }
                }
            }
        }
    }

    private func rowView(cells: [InlineContent], isHeader: Bool) -> some View {
        HStack(alignment: .top, spacing: 0) {
            ForEach(Array(cells.enumerated()), id: \.offset) { cIdx, cell in
                let align = cIdx < table.columnAlignment.count
                    ? table.columnAlignment[cIdx]
                    : .none
                MarkdownInlineBlockView(content: cell)
                    .font(isHeader ? .subheadline.weight(.semibold) : .subheadline)
                    .frame(maxWidth: 180, alignment: cellAlignment(align))
                    .padding(6)
                    .frame(maxWidth: .infinity, alignment: cellAlignment(align))
                if cIdx < cells.count - 1 {
                    Rectangle()
                        .fill(Color.secondary.opacity(0.25))
                        .frame(width: 1)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(isHeader ? Color.gray.opacity(0.14) : Color.clear)
    }

    /// `frame(alignment:)` takes `Alignment`, not `HorizontalAlignment`.
    private func cellAlignment(_ a: TableBlock.ColumnAlignment) -> Alignment {
        switch a {
        case .left: return .leading
        case .center: return .center
        case .right: return .trailing
        case .none: return .leading
        }
    }
}
