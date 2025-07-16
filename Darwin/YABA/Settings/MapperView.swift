//
//  MapperView.swift
//  YABA
//
//  Created by Ali Taha on 24.05.2025.
//

import UniformTypeIdentifiers
import SwiftUI

struct MapperView: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Environment(\.modelContext)
    private var modelContext
    
    @Binding
    var settingsState: SettingsState
    
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .accentColor)
            content
        }
        .navigationTitle(
            settingsState.importedFileType == .json
            ? "Map JSON Label"
            : settingsState.importedFileType == .html
            ? "Map HTML Label"
            : "Map CSV Label"
        )
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(role: .cancel) {
                    dismiss()
                } label: {
                    if settingsState.importedFileType == .json || settingsState.importedFileType == .html {
                        YabaIconView(bundleKey: "arrow-left-01")
                    } else if settingsState.importedFileType == .commaSeparatedText {
                        Text("Cancel")
                    }
                }.disabled(settingsState.isImporting)
            }
            if settingsState.importedFileType == .commaSeparatedText {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        settingsState.importFixedCSV(using: modelContext)
                    } label: {
                        HStack {
                            Text("Import")
                            if settingsState.isImporting {
                                ProgressView().controlSize(.regular)
                            }
                        }
                    }.disabled(settingsState.mappedHeaders[.url] == nil || settingsState.isImporting)
                }
            }
        }
        .onDisappear { settingsState.reset() }
    }
    
    @ViewBuilder
    private var content: some View {
        List {
            if settingsState.importedFileType == .commaSeparatedText {
                Section {
                    generateMapperSection(
                        iconBundleKey: "link-02",
                        label: "Bookmark URL Placeholder",
                        value: .url
                    )
                    generateMapperSection(
                        iconBundleKey: "text",
                        label: "Bookmark Title Placeholder",
                        value: .label
                    )
                    generateMapperSection(
                        iconBundleKey: "paragraph",
                        label: "Bookmark Description Placeholder",
                        value: .description
                    )
                    generateMapperSection(
                        iconBundleKey: "clock-01",
                        label: "Bookmark Detail Created At Title",
                        value: .createdAt
                    )
                } header: {
                    Label {
                        Text("Mapping Label")
                    } icon: {
                        YabaIconView(bundleKey: "maps")
                            .scaledToFit()
                            .frame(width: 18, height: 18)
                    }
                }
            }
            Section {
                VStack(alignment: .leading) {
                    if settingsState.importedFileType == .commaSeparatedText {
                        Text("Map CSV Info Section Text")
                        HStack {
                            Text("Mapper Info Section CTA Text")
                            Spacer()
                        }
                        CSVInfoListView()
                        TypeInfoView(forCollections: false)
                    } else if settingsState.importedFileType == .json {
                        Text("Map JSON Info Section Text")
                        HStack {
                            Text("Mapper Info Section CTA Text")
                            Spacer()
                            YabaIconView(bundleKey: "copy-01")
                                .frame(width: 22, height: 22)
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    UIPasteboard.general.string = Constants.jsonExample
                                    settingsState.toastManager.show(
                                        message: LocalizedStringKey("Copy To Clipboard Success Message"),
                                        accentColor: .blue,
                                        acceptText: LocalizedStringKey("Ok"),
                                        iconType: .hint,
                                        onAcceptPressed: { settingsState.toastManager.hide() }
                                    )
                                }
                        }
                        JSONCodeBlockView()
                        TypeInfoView(forCollections: true)
                        TypeInfoView(forCollections: false)
                    } else if settingsState.importedFileType == .html {
                        HTMLInfoListView()
                    }
                }
            } header: {
                Label {
                    Text("Info")
                } icon: {
                    YabaIconView(bundleKey: "information-circle")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
            }
        }
        .listStyle(.sidebar)
        .scrollContentBackground(.hidden)
        .background(.clear)
    }
    
    @ViewBuilder
    private func generateMapperSection(
        iconBundleKey: String,
        label: String,
        value: MappableCSVHeaderValues
    ) -> some View {
        HStack {
            YabaIconView(bundleKey: iconBundleKey)
                .scaledToFit()
                .frame(width: 20, height: 20)
                .foregroundStyle(.tint)
            Text(LocalizedStringKey(label))
            Spacer()
            Menu {
                ForEach(settingsState.importedHeaders.indices, id: \.self) { headerIndex in
                    Button {
                        settingsState.mappedHeaders[value] = headerIndex
                    } label: {
                        Text(settingsState.importedHeaders[headerIndex])
                    }
                }
                Divider()
                Button {
                    settingsState.mappedHeaders[value] = nil
                } label: {
                    Text("None Mapper Label")
                }
            } label: {
                Label {
                    if settingsState.mappedHeaders[value] == nil {
                        Text("None Mapper Label")
                    } else {
                        if let titleIndex = settingsState.mappedHeaders[value] ?? nil {
                            Text(settingsState.importedHeaders[titleIndex])
                        } else {
                            Text("Select Mapper Label")
                        }
                    }
                } icon: {
                    YabaIconView(bundleKey: "arrow-right-01")
                        .scaledToFit()
                        .frame(width: 16, height: 16)
                        .foregroundStyle(.tint)
                }.labelStyle(InverseLabelStyle())
            }
        }
    }
}

/// MARK: JSON
/// Taken from: https://medium.com/@orhanerday/building-a-swiftui-code-block-view-with-syntax-highlighting-d3d737a90a65
private struct JSONCodeBlockView: View {
    var body: some View {
        ScrollView(.horizontal) {
            Text(attributedJSONString(Constants.jsonExample))
                .font(.system(.body, design: .monospaced))
                .background {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(.gray.opacity(0.1))
                }
                .multilineTextAlignment(.leading)
        }
        .padding(.horizontal)
    }
    
    private func attributedJSONString(_ json: String) -> AttributedString {
        var attributedString = AttributedString(json)
        
        // Match keys: "key":
        let keyPattern = #"\"(.*?)\"\s*:"#  // double quoted key followed by colon
        // Match string values: "value"
        let stringPattern = #"\"([^"]*)\"(?=\s*[,\}])"#
        // Match numbers
        let numberPattern = #"(?<=:\s|,\s|{\s|[\[])\d+(\.\d+)?(?=\s*[,\}\]])"#
        // Match booleans & null
        let literalPattern = #"\b(true|false|null)\b"#
        
        func highlight(pattern: String, color: Color) {
            guard let regex = try? NSRegularExpression(pattern: pattern) else { return }
            let nsRange = NSRange(json.startIndex..<json.endIndex, in: json)
            let matches = regex.matches(in: json, range: nsRange)
            
            for match in matches {
                if let range = Range(match.range, in: json),
                   let attrRange = Range(NSRange(range, in: json), in: attributedString) {
                    attributedString[attrRange].foregroundColor = color
                }
            }
        }
        
        highlight(pattern: keyPattern, color: .blue)       // Keys
        highlight(pattern: stringPattern, color: .green)   // String values
        highlight(pattern: numberPattern, color: .orange)  // Numbers
        highlight(pattern: literalPattern, color: .purple) // true/false/null
        
        return attributedString
    }
}

/// MARK: CSV
private struct CSVField: Identifiable {
    var id: Int { column }
    let column: Int
    let field: String
    let notes: LocalizedStringKey
}

private struct CSVInfoListView: View {
    private let csvFields: [CSVField] = [
        .init(column: 1, field: "bookmarkId",          notes: "CSV Notes Bookmark ID"),
        .init(column: 2, field: "label",               notes: "CSV Notes Label"),
        .init(column: 3, field: "bookmarkDescription", notes: "CSV Notes Optional"),
        .init(column: 4, field: "link",                notes: "CSV Notes Link"),
        .init(column: 5, field: "domain",              notes: "CSV Notes Optional"),
        .init(column: 6, field: "createdAt",           notes: "CSV Notes Created At"),
        .init(column: 7, field: "editedAt",            notes: "CSV Notes Edited At"),
        .init(column: 8, field: "imageUrl",            notes: "CSV Notes Optional"),
        .init(column: 9, field: "iconUrl",             notes: "CSV Notes Optional"),
        .init(column: 10, field: "videoUrl",           notes: "CSV Notes Optional"),
        .init(column: 11, field: "type",               notes: "CSV Notes Type"),
        .init(column: 12, field: "version",            notes: "CSV Notes Optional"),
    ]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(csvFields) { field in
                VStack(alignment: .leading, spacing: 4) {
                    Text(LocalizedStringKey("Map CSV Column Header Label \(field.column) \(field.field)"))
                        .font(.system(.body, design: .monospaced))
                        .bold()
                    Text(field.notes)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
    }
}

/// MARK: HTML
private struct HTMLInfoListView: View {
    var body: some View {
        VStack(alignment: .leading) {
            Text("Map HTML Info Section Text")
        }
    }
}

/// MARK: BOOKMARK AND FOLDER TYPE
private struct TypeInfo: Identifiable {
    let id: Int
    let title: LocalizedStringKey
    let description: LocalizedStringKey
    
    static func getBookmarkInfo() -> [TypeInfo] {
        return [
            .init(id: BookmarkType.none.rawValue, title: "Bookmark Type None", description: "Bookmark Type None Description"),
            .init(id: BookmarkType.webLink.rawValue, title: "Bookmark Type Link", description: "Bookmark Type Link Description"),
            .init(id: BookmarkType.video.rawValue, title: "Bookmark Type Video", description: "Bookmark Type Video Description"),
            .init(id: BookmarkType.image.rawValue, title: "Bookmark Type Image", description: "Bookmark Type Image Description"),
            .init(id: BookmarkType.audio.rawValue, title: "Bookmark Type Audio", description: "Bookmark Type Music Description"),
            .init(id: BookmarkType.music.rawValue, title: "Bookmark Type Music", description: "Bookmark Type Audio Description")
        ]
    }
    
    static func getCollectionInfo() -> [TypeInfo] {
        return [
            .init(
                id: CollectionType.folder.rawValue,
                title: "Folder",
                description: LocalizedStringKey("Collection Type Folder Description")
            ),
            .init(
                id: CollectionType.tag.rawValue,
                title: "Tag",
                description: LocalizedStringKey("Collection Type Tag Description")
            )
        ]
    }
}

private struct TypeInfoView: View {
    let forCollections: Bool
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(
                LocalizedStringKey(
                    forCollections
                    ? "Type Guide Collection Label"
                    : "Type Guide Bookmark Label"
                )
            ).font(.headline)
            
            ForEach(forCollections ? TypeInfo.getCollectionInfo() : TypeInfo.getBookmarkInfo()) { type in
                HStack(alignment: .top, spacing: 6) {
                    Text("\(type.id)")
                        .bold()
                        .frame(width: 24, alignment: .leading)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(type.title)
                            .bold()
                        Text(type.description)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .padding()
    }
}

#Preview {
    MapperView(settingsState: .constant(.init()))
}
