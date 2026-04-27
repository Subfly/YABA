//
//  MarkdownListBlockViews.swift
//  YABACore
//
//  Render-model list blocks.
//

import SwiftUI

// MARK: - List

struct MarkdownListBlockView: View {
    let list: ListBlockModel
    let configuration: MarkdownPreviewConfiguration
    var listNesting: Int

    var body: some View {
        VStack(alignment: .leading, spacing: list.tight ? 0 : 4) {
            ForEach(Array(list.items.enumerated()), id: \.offset) { idx, item in
                MarkdownListItemView(
                    item: item,
                    index: list.ordered ? (list.startNumber + idx) : nil,
                    bullet: list.bullet,
                    ordered: list.ordered,
                    configuration: configuration,
                    listNesting: listNesting
                )
            }
        }
        .padding(.leading, CGFloat(listNesting) * 16)
    }
}

struct MarkdownListItemView: View {
    let item: ListItemBlock
    let index: Int?
    let bullet: Character
    let ordered: Bool
    let configuration: MarkdownPreviewConfiguration
    var listNesting: Int

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            listMarker
            VStack(alignment: .leading, spacing: 4) {
                ForEach(item.children) { child in
                    if case .list(let inner) = child.kind {
                        MarkdownListBlockView(
                            list: inner,
                            configuration: configuration,
                            listNesting: listNesting + 1
                        )
                    } else {
                        MarkdownBlockView(
                            block: child,
                            configuration: configuration,
                            listNesting: listNesting
                        )
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var listMarker: some View {
        if item.isTask {
            Image(systemName: item.checked ? "checkmark.square.fill" : "square")
                .foregroundColor(.secondary)
        } else if ordered, let n = index {
            Text("\(n).")
        } else {
            Text(verbatim: String(bullet))
        }
    }
}
