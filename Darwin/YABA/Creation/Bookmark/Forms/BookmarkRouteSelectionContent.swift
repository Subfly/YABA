//
//  BookmarkRouteSelectionContent.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftUI

struct BookmarkRouteSelectionContent: View {
    let onCancel: () -> Void
    let onSelectKind: (BookmarkKind) -> Void

    private struct RowSpec: Identifiable {
        let id: BookmarkKind
        let title: String
        let iconKey: String
        let color: YabaColor
    }

    private static let rows: [RowSpec] = [
        RowSpec(id: .link, title: "New Link", iconKey: "link-02", color: .blue),
        RowSpec(id: .note, title: "New Note", iconKey: "note-edit", color: .yellow),
        RowSpec(id: .image, title: "New Image", iconKey: "image-03", color: .green),
        RowSpec(id: .file, title: "New Document", iconKey: "doc-02", color: .red),
        RowSpec(id: .canvas, title: "New Canvas", iconKey: "canvas", color: .orange)
    ]

    var body: some View {
        NavigationStack {
            List {
                ForEach(Self.rows, id: \.id) { row in
                    generateRouteButton(row: row)
                }
            }
            .listStyle(.sidebar)
            #if !os(visionOS)
            .scrollDismissesKeyboard(.immediately)
            #endif
            .scrollContentBackground(.hidden)
            .navigationTitle("New Bookmark")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .cancel, action: onCancel) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func generateRouteButton(row: RowSpec) -> some View {
        Button {
            onSelectKind(row.id)
        } label: {
            HStack {
                YabaIconView(bundleKey: row.iconKey)
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundStyle(row.color.getUIColor())
                    .padding(.trailing, 12)
                Text(row.title)
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 22, height: 22)
                    .foregroundStyle(.secondary)
            }
        }
        .buttonStyle(.plain)
    }
}
