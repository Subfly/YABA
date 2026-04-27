//
//  MarkdownKMPListBlockViews.swift
//  YABACore
//
//  KMP `ListBlock` / `ListItem` rendering.
//

import MarkdownParser
import SwiftUI

struct MarkdownKMPListBlockView: View {
    let list: ListBlock
    var listNesting: Int

    var body: some View {
        let items = list.children.compactMap { $0 as? ListItem }
        VStack(alignment: .leading, spacing: list.tight ? 0 : 4) {
            ForEach(Array(items.enumerated()), id: \.offset) { idx, li in
                MarkdownKMPListItemView(
                    item: li,
                    index: list.ordered ? (Int(list.startNumber) + idx) : nil,
                    bullet: Character(UnicodeScalar(UInt32(list.bulletChar))!),
                    ordered: list.ordered,
                    listNesting: listNesting
                )
            }
        }
        .padding(.leading, CGFloat(listNesting) * 16)
    }
}

struct MarkdownKMPListItemView: View {
    let item: ListItem
    let index: Int?
    let bullet: Character
    let ordered: Bool
    var listNesting: Int

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            listMarker
            VStack(alignment: .leading, spacing: 4) {
                ForEach(Array(listItemChildNodes.enumerated()), id: \.offset) { _, ch in
                    if let inner = ch as? ListBlock {
                        MarkdownKMPListBlockView(list: inner, listNesting: listNesting + 1)
                    } else {
                        MarkdownKMPNodeBlockView(node: ch, tableIndex: 0, listNesting: listNesting)
                    }
                }
            }
        }
    }

    private var listItemChildNodes: [Node] {
        (item as ContainerNode).children.filter { !($0 is BlankLine) }
    }

    @ViewBuilder
    private var listMarker: some View {
        if item.taskListItem {
            Image(systemName: item.checked ? "checkmark.square.fill" : "square")
                .foregroundColor(.secondary)
        } else if ordered, let n = index {
            Text("\(n).")
        } else {
            Text(verbatim: String(bullet))
        }
    }
}
