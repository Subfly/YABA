//
//  Created by Ali Taha on 20.04.2026.
//

import SwiftData
import SwiftUI

enum LinkmarkDetailSheetTab: String, CaseIterable, Identifiable, Hashable {
    case info
    case versions
    case annotations
    case contents

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .info: return "information-circle"
        case .versions: return "clock-02"
        case .annotations: return "sticky-note-03"
        case .contents: return "align-box-middle-center"
        }
    }

    var title: LocalizedStringKey {
        switch self {
        case .info: return "Bookmark Detail Sheet Tab Info Title"
        case .versions: return "Bookmark Detail Sheet Tab Versions Title"
        case .annotations: return "Bookmark Detail Sheet Tab Annotations Title"
        case .contents: return "Bookmark Detail Sheet Tab Contents Title"
        }
    }
}

struct LinkmarkDetailInfoSheet: View {
    @Environment(\.dismiss)
    private var dismiss

    let bookmark: YabaBookmark
    let toc: Toc?
    let folderAccent: Color
    @Binding var selectedTab: LinkmarkDetailSheetTab
    let sortedVersions: [ReadableVersionModel]
    let selectedVersionId: String?
    let onSelectVersion: (String) -> Void
    let onDeleteVersion: (String) -> Void
    let onTocItemTap: (TocItem) -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("", selection: $selectedTab) {
                    ForEach(LinkmarkDetailSheetTab.allCases) { tab in
                        Text(tab.title).tag(tab)
                    }
                }
                .pickerStyle(.segmented)
                .padding()

                content
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            }
            .navigationTitle("Bookmark Detail Sheet Navigation Title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch selectedTab {
        case .info:
            linkInfoScroll
        case .versions:
            versionsList
        case .annotations:
            annotationsList
        case .contents:
            tocList
        }
    }

    private var linkInfoScroll: some View {
        List {
            Section {
                Text(bookmark.label)
                    .font(.headline)
                if let desc = bookmark.bookmarkDescription, !desc.isEmpty {
                    Text(desc)
                        .foregroundStyle(.secondary)
                }
            } header: {
                Label {
                    Text("Info")
                } icon: {
                    YabaIconView(bundleKey: "information-circle")
                        .frame(width: 22, height: 22)
                }
            }

            Section {
                metadataRow("Bookmark Detail URL Label", icon: "link-02", value: bookmark.linkDetail?.url)
                metadataRow("Bookmark Detail Metadata Title Label", icon: "text", value: bookmark.linkDetail?.metadataTitle)
                metadataRow("Bookmark Creation Metadata Description Label", icon: "paragraph", value: bookmark.linkDetail?.metadataDescription)
            } header: {
                Label {
                    Text("Bookmark Creation Metadata Section Title")
                } icon: {
                    YabaIconView(bundleKey: "database-01")
                        .frame(width: 22, height: 22)
                }
            }

            if let folder = bookmark.folder {
                Section {
                    HStack(alignment: .center, spacing: 12) {
                        YabaIconView(bundleKey: "folder-01")
                            .frame(width: 22, height: 22)
                            .foregroundStyle(folderAccent)
                        Text(folder.label)
                    }
                } header: {
                    Text("Folder")
                }
            }

            if !bookmark.tags.isEmpty {
                Section {
                    HStack(alignment: .center, spacing: 12) {
                        YabaIconView(bundleKey: "tag-01")
                            .frame(width: 22, height: 22)
                            .foregroundStyle(folderAccent)
                        Text(bookmark.tags.map(\.label).joined(separator: ", "))
                    }
                } header: {
                    Text("Tags Title")
                }
            }
        }
        .listStyle(.sidebar)
        .scrollContentBackground(.hidden)
    }

    private var versionsList: some View {
        Group {
            if sortedVersions.isEmpty {
                ContentUnavailableView("Bookmark Detail No Versions Title", systemImage: "clock")
            } else {
                List {
                    ForEach(sortedVersions, id: \.readableVersionId) { v in
                        Button {
                            onSelectVersion(v.readableVersionId)
                        } label: {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(v.createdAt.formatted())
                                    Text(v.readableVersionId)
                                        .font(.caption2)
                                        .foregroundStyle(.tertiary)
                                }
                                Spacer()
                                if v.readableVersionId == selectedVersionId
                                    || (selectedVersionId == nil && v.readableVersionId == sortedVersions.first?.readableVersionId)
                                {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundStyle(folderAccent)
                                }
                            }
                        }
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                onDeleteVersion(v.readableVersionId)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            }
        }
    }

    private var annotationsList: some View {
        let versionId = selectedVersionId
            ?? sortedVersions.first?.readableVersionId
        let items = bookmark.annotations.filter { ann in
            guard let versionId else { return true }
            return ann.readableVersion?.readableVersionId == versionId
        }
        return Group {
            if items.isEmpty {
                ContentUnavailableView("Bookmark Detail No Annotations Title", systemImage: "note.text")
            } else {
                List(items, id: \.annotationId) { a in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(a.quoteText ?? a.note ?? "")
                            .lineLimit(4)
                        Text(a.annotationId)
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                    }
                }
            }
        }
    }

    private var tocList: some View {
        Group {
            let rows = Self.flattenToc(toc?.items ?? [])
            if rows.isEmpty {
                ContentUnavailableView("Bookmark Detail No Table Of Contents Title", systemImage: "list.bullet")
            } else {
                List {
                    ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                        Button {
                            onTocItemTap(row.item)
                        } label: {
                            Text(row.item.title)
                                .padding(.leading, CGFloat(row.level) * 12)
                        }
                    }
                }
            }
        }
    }

    private static func flattenToc(_ items: [TocItem], level: Int = 0) -> [(item: TocItem, level: Int)] {
        var out: [(item: TocItem, level: Int)] = []
        for item in items {
            out.append((item, level))
            if !item.children.isEmpty {
                out.append(contentsOf: flattenToc(item.children, level: level + 1))
            }
        }
        return out
    }

    @ViewBuilder
    private func metadataRow(_ key: LocalizedStringKey, icon: String, value: String?) -> some View {
        if let value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            HStack(alignment: .top, spacing: 12) {
                YabaIconView(bundleKey: icon)
                    .frame(width: 22, height: 22)
                    .foregroundStyle(folderAccent)
                    .padding(.top, 2)
                VStack(alignment: .leading, spacing: 2) {
                    Text(key)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(value)
                        .font(.body)
                }
            }
        }
    }
}
