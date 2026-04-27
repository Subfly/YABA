//
//  MarkdownInDocumentTabView.swift
//  YABACore
//
//  In-document tab picker (not SwiftUI `TabView` pages).
//

import SwiftUI

// MARK: - In-document tabs (not SwiftUI `TabView` pages)

struct MarkdownInDocumentTabView: View {
    let tabs: [TabItemBlock]
    let configuration: MarkdownPreviewConfiguration
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
                MarkdownBlockStackView(blocks: tabs[index].children, listNesting: listNesting)
            }
        }
    }
}
