//
//  MarkdownKMPInDocumentTabView.swift
//  YABACore
//
//  KMP `TabBlock` → in-document tab picker.
//

import MarkdownParser
import SwiftUI

struct KmpTab {
    let title: String
    let nodes: [Node]
}

func collectKmpTabs(_ n: TabBlock) -> [KmpTab] {
    var tabs: [KmpTab] = []
    for c in n.children {
        guard let t = c as? TabItem else { continue }
        let nodes = (t as ContainerNode).children.filter { !($0 is BlankLine) }
        tabs.append(KmpTab(title: t.title, nodes: nodes))
    }
    return tabs
}

struct MarkdownKMPInDocumentTabView: View {
    let tabs: [KmpTab]
    var listNesting: Int
    @State private var index: Int = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if tabs.count <= 4 {
                Picker("Tab", selection: $index) {
                    ForEach(tabs.indices, id: \.self) { i in
                        Text(tabs[i].title).lineLimit(1).tag(i)
                    }
                }
                .pickerStyle(.segmented)
            } else {
                Picker("Tab", selection: $index) {
                    ForEach(tabs.indices, id: \.self) { i in
                        Text(tabs[i].title).tag(i)
                    }
                }
            }
            if tabs.indices.contains(index) {
                MarkdownKMPBlockStackView(nodes: tabs[index].nodes, listNesting: listNesting)
            }
        }
    }
}
