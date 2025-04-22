//
//  SelectFolderContent.swift
//  YABA
//
//  Created by Ali Taha on 22.04.2025.
//

import SwiftUI
import SwiftData

struct SelectFolderContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @State
    private var showFolderCreationSheet: Bool = false
    
    @State
    private var searchQuery: String = ""
    
    @Binding
    var selectedFolder: YabaCollection?
    
    var body: some View {
        SelectFolderSearchableContent(
            selectedFolder: $selectedFolder,
            searchQuery: $searchQuery
        )
        .scrollDismissesKeyboard(.immediately)
        .searchable(text: $searchQuery, prompt: "Folder Search Prompt")
        .navigationTitle("Select Folder Title")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showFolderCreationSheet = true
                } label: {
                    Image(systemName: "folder.badge.plus")
                }
            }
        }
        .sheet(isPresented: $showFolderCreationSheet) {
            CollectionCreationContent(
                collectionType: .folder,
                collectionToEdit: .constant(nil),
                onEditCallback: { _ in }
            )
        }
    }
}

private struct SelectFolderSearchableContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Query
    private var allFolders: [YabaCollection]
    
    @Binding
    var selectedFolder: YabaCollection?
    
    @Binding
    var searchQuery: String
    
    init(
        selectedFolder: Binding<YabaCollection?>,
        searchQuery: Binding<String>
    ) {
        let compareValue = CollectionType.folder.rawValue
        let query = searchQuery.wrappedValue
        _allFolders = Query(
            filter: #Predicate<YabaCollection> {
                if query.isEmpty {
                    $0.type == compareValue
                } else {
                    $0.type == compareValue
                    && $0.label.localizedStandardContains(query)
                }
            }
        )
        _selectedFolder = selectedFolder
        _searchQuery = searchQuery
    }
    
    var body: some View {
        List {
            if allFolders.isEmpty {
                if searchQuery.isEmpty {
                    ContentUnavailableView(
                        "Select Folder No Folders Available Title",
                        systemImage: "folder",
                        description: Text(
                            LocalizedStringKey(
                                "Select Folder No Folders Available Description"
                            )
                        )
                    )
                } else {
                    ContentUnavailableView(
                        "Select Folder No Folder Found In Search Title",
                        systemImage: "magnifyingglass",
                        description: Text(
                            LocalizedStringKey(
                                "Select Folder No Folder Found In Search Description \(searchQuery)"
                            )
                        )
                    )
                }
            } else {
                ForEach(allFolders) { folder in
                    CollectionItemView(
                        collection: folder,
                        selectedCollection: .constant(nil),
                        isInSelectionMode: true,
                        onDeleteCallback: { collection in
                            withAnimation {
                                if selectedFolder?.id == collection.id {
                                    selectedFolder = nil
                                }
                            }
                        },
                        onEditCallback: { collection in
                            withAnimation {
                                if selectedFolder?.id == collection.id {
                                    selectedFolder = collection
                                }
                            }
                        }
                    )
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation {
                            dismiss()
                            selectedFolder = folder
                        }
                    }
                }
            }
        }
    }
}

#Preview {
    SelectFolderContent(selectedFolder: .constant(nil))
}
