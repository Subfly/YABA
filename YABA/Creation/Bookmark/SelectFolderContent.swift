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
            searchQuery: searchQuery
        )
        .scrollDismissesKeyboard(.immediately)
        .searchable(text: $searchQuery, prompt: "Folder Search Prompt")
        .navigationTitle("Select Folder Title")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button {
                    dismiss()
                } label: {
                    Text("Cancel")
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showFolderCreationSheet = true
                } label: {
                    Text("New")
                }
            }
        }
        .sheet(isPresented: $showFolderCreationSheet) {
            CollectionCreationContent(
                collectionType: .folder,
                collectionToEdit: .constant(nil)
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
    
    init(
        selectedFolder: Binding<YabaCollection?>,
        searchQuery: String
    ) {
        let compareValue = CollectionType.folder.rawValue
        _allFolders = Query(
            filter: #Predicate<YabaCollection> {
                if searchQuery.isEmpty {
                    $0.type == compareValue
                } else {
                    $0.type == compareValue && $0.label.localizedStandardContains(searchQuery)
                }
            }
        )
        _selectedFolder = selectedFolder
    }
    
    var body: some View {
        List {
            ForEach(allFolders) { folder in
                CollectionItemView(
                    collection: folder,
                    selectedCollection: .constant(nil),
                    isInSelectionMode: true
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

#Preview {
    SelectFolderContent(selectedFolder: .constant(nil))
}
